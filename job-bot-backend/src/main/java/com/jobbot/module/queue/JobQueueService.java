package com.jobbot.module.queue;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.discovery.ApplicationCapability;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.platform.PlatformConfigService;
import com.jobbot.module.resume.variant.ResumeSelectionService;
import com.jobbot.module.resume.variant.VariantScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The application queue — high-scoring discovered postings await user review
 * before the local application-engine (or Chrome extension for LinkedIn) applies
 * to them. Rate-limited per-platform via {@link PlatformConfigService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final JobQueueRepository repository;
    private final PlatformConfigService platformConfigService;
    private final ApplicationRepository applicationRepository;
    private final ResumeSelectionService resumeSelectionService;

    // ---------- Enqueue ----------

    /**
     * Add a posting to the queue as PENDING_REVIEW if it doesn't already exist.
     * Returns the (existing or new) entry. If the posting is UNAVAILABLE or
     * filtered out (score below criteria threshold, or SKIP recommendation),
     * the entry is created with status FILTERED_OUT so the user still sees the
     * decision trail.
     */
    @Transactional
    public JobQueueEntry enqueueFromPosting(JobPosting posting,
                                            BigDecimal minMatchScore) {
        Optional<JobQueueEntry> existing = repository.findByExternalIdAndPlatform(
                posting.getExternalId(), posting.getSource().name());
        if (existing.isPresent()) return existing.get();

        JobQueueStatus status = JobQueueStatus.PENDING_REVIEW;
        String reason = null;
        BigDecimal match = posting.getMatchScore() != null
                ? BigDecimal.valueOf(posting.getMatchScore()) : null;
        if ("SKIP".equalsIgnoreCase(posting.getRecommendation())) {
            status = JobQueueStatus.FILTERED_OUT;
            reason = "Recommendation = SKIP";
        } else if (minMatchScore != null && match != null
                && match.compareTo(minMatchScore) < 0) {
            status = JobQueueStatus.FILTERED_OUT;
            reason = "Score " + match + " < threshold " + minMatchScore;
        } else if (posting.getApplicationCapability() == ApplicationCapability.UNAVAILABLE) {
            status = JobQueueStatus.FILTERED_OUT;
            reason = "Posting no longer available";
        }

        String variant = null;
        try {
            VariantScore best = resumeSelectionService.selectBest(posting);
            variant = best.variant().name();
        } catch (Exception ignored) { /* best-effort */ }

        JobQueueEntry entry = JobQueueEntry.builder()
                .jobPostingId(posting.getId())
                .externalId(posting.getExternalId())
                .platform(posting.getSource() != null ? posting.getSource().name() : "OTHER")
                .title(posting.getTitle())
                .company(posting.getCompany())
                .location(posting.getLocation())
                .jobUrl(posting.getSourceUrl())
                .description(posting.getDescription())
                .matchScore(match)
                .recommendation(posting.getRecommendation())
                .resumeVariant(variant)
                .status(status)
                .failureReason(reason)
                .build();
        JobQueueEntry saved = repository.save(entry);
        if (status == JobQueueStatus.PENDING_REVIEW) {
            log.info("Enqueued for review: '{}' @ {} ({} match={})",
                    saved.getTitle(), saved.getCompany(), saved.getPlatform(), match);
        }
        return saved;
    }

    // ---------- User actions ----------

    @Transactional
    public JobQueueEntry approve(UUID id) {
        JobQueueEntry e = get(id);
        e.setStatus(JobQueueStatus.APPROVED);
        e.setReviewedAt(OffsetDateTime.now());
        return repository.save(e);
    }

    @Transactional
    public JobQueueEntry skip(UUID id) {
        JobQueueEntry e = get(id);
        e.setStatus(JobQueueStatus.SKIPPED);
        e.setReviewedAt(OffsetDateTime.now());
        return repository.save(e);
    }

    @Transactional
    public JobQueueEntry sendToManual(UUID id) {
        JobQueueEntry e = get(id);
        e.setStatus(JobQueueStatus.MANUAL_APPLY);
        e.setReviewedAt(OffsetDateTime.now());
        return repository.save(e);
    }

    /** User applied manually → creates a CRM {@code Application} (autoApplied=false). */
    @Transactional
    public JobQueueEntry markApplied(UUID id) {
        JobQueueEntry e = get(id);
        if (e.getStatus() == JobQueueStatus.APPLIED) return e;
        createApplication(e, false);
        e.setStatus(JobQueueStatus.APPLIED);
        e.setAppliedAt(OffsetDateTime.now());
        return repository.save(e);
    }

    /** Approve every PENDING_REVIEW entry whose match score is ≥ threshold. */
    @Transactional
    public int approveAllAbove(BigDecimal threshold) {
        List<JobQueueEntry> hits = repository.findPendingAboveThreshold(threshold);
        int n = 0;
        OffsetDateTime now = OffsetDateTime.now();
        for (JobQueueEntry e : hits) {
            e.setStatus(JobQueueStatus.APPROVED);
            e.setReviewedAt(now);
            n++;
        }
        repository.saveAll(hits);
        log.info("Bulk-approved {} entries above {}", n, threshold);
        return n;
    }

    // ---------- Engine (application-engine + Chrome extension) ----------

    /**
     * Fetch the next APPROVED job for a platform and atomically flip it to
     * AUTO_APPLYING so no other worker picks it up.
     */
    @Transactional
    public Optional<JobQueueEntry> pickNextApproved(String platform) {
        String p = platform == null ? null : platform.toUpperCase(Locale.ROOT);
        if (p == null) return Optional.empty();
        if (!platformConfigService.canApply(p)) return Optional.empty();
        List<JobQueueEntry> next = repository.findApprovedForPlatform(p, PageRequest.of(0, 1));
        if (next.isEmpty()) return Optional.empty();
        JobQueueEntry e = next.get(0);
        e.setStatus(JobQueueStatus.AUTO_APPLYING);
        return Optional.of(repository.save(e));
    }

    /** Engine reports success → creates CRM Application(autoApplied=true) + increments rate counter. */
    @Transactional
    public JobQueueEntry markAutoApplied(UUID id) {
        JobQueueEntry e = get(id);
        createApplication(e, true);
        e.setStatus(JobQueueStatus.APPLIED);
        e.setAppliedAt(OffsetDateTime.now());
        platformConfigService.incrementCount(e.getPlatform());
        log.info("Auto-applied: '{}' @ {} ({})", e.getTitle(), e.getCompany(), e.getPlatform());
        return repository.save(e);
    }

    /**
     * Engine reports failure. Failures containing CAPTCHA / BLOCKED move the
     * entry to MANUAL_APPLY so the user can complete it; everything else goes
     * to FAILED_APPLY.
     */
    @Transactional
    public JobQueueEntry markFailed(UUID id, String reason) {
        JobQueueEntry e = get(id);
        String upper = reason == null ? "" : reason.toUpperCase(Locale.ROOT);
        boolean requiresHuman = upper.contains("CAPTCHA") || upper.contains("BLOCKED")
                || upper.contains("LOGIN") || upper.contains("2FA");
        e.setStatus(requiresHuman ? JobQueueStatus.MANUAL_APPLY : JobQueueStatus.FAILED_APPLY);
        e.setFailureReason(reason);
        log.warn("Apply failed for '{}' @ {} → {} ({})",
                e.getTitle(), e.getCompany(), e.getStatus(), reason);
        return repository.save(e);
    }

    // ---------- Reads ----------

    public Page<JobQueueEntry> getPendingReview(Pageable p) {
        return repository.findByStatusOrderByMatchScoreDescCreatedAtDesc(
                JobQueueStatus.PENDING_REVIEW, p);
    }

    public Page<JobQueueEntry> getAutoApplying(Pageable p) {
        return repository.findByStatusInOrderByMatchScoreDescCreatedAtDesc(
                List.of(JobQueueStatus.APPROVED, JobQueueStatus.AUTO_APPLYING), p);
    }

    public Page<JobQueueEntry> getManualQueue(Pageable p) {
        return repository.findByStatusInOrderByMatchScoreDescCreatedAtDesc(
                List.of(JobQueueStatus.MANUAL_APPLY, JobQueueStatus.FAILED_APPLY), p);
    }

    public Map<String, Long> stats() {
        Map<String, Long> out = new HashMap<>();
        for (JobQueueStatus s : JobQueueStatus.values()) {
            out.put(s.name(), repository.countByStatus(s));
        }
        return out;
    }

    // ---------- helpers ----------

    private JobQueueEntry get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Job queue entry not found: " + id));
    }

    private void createApplication(JobQueueEntry e, boolean auto) {
        Application app = Application.builder()
                .company(e.getCompany())
                .title(e.getTitle())
                .platform(e.getPlatform())
                .status("applied")
                .atsScore(e.getMatchScore())
                .autoApplied(auto)
                .jobQueueId(e.getId())
                .appliedAt(OffsetDateTime.now())
                .build();
        applicationRepository.save(app);
    }

    /** Convenience helper for the top-scored entries used by the queue dashboard. */
    public List<JobQueueEntry> topPendingReview(int n) {
        return getPendingReview(PageRequest.of(0, n)).getContent().stream()
                .sorted(Comparator.comparing(
                        (JobQueueEntry q) -> q.getMatchScore() == null ? BigDecimal.ZERO : q.getMatchScore()
                ).reversed())
                .toList();
    }
}

