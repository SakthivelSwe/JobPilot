package com.jobbot.module.manualqueue;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateProfileService;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.matching.JobMatchService;
import com.jobbot.module.matching.MatchResult;
import com.jobbot.module.resume.variant.ResumeSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manual application queue (spec §27). Holds high-quality jobs that JobPilot cannot
 * (or must not) submit automatically, so the user applies manually. "Mark applied"
 * carries the job into the existing Kanban CRM (spec §29). Never auto-submits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualQueueService {

    private final ManualQueueRepository repository;
    private final JobPostingRepository postingRepository;
    private final CandidateProfileService profileService;
    private final JobMatchService matchService;
    private final ResumeSelectionService selectionService;
    private final ApplicationRepository applicationRepository;

    /** Add a posting to the manual queue (idempotent per posting). */
    @Transactional
    public ManualQueueEntry add(UUID postingId) {
        Optional<ManualQueueEntry> existing = repository.findByPostingId(postingId);
        if (existing.isPresent()) return existing.get();

        JobPosting posting = postingRepository.findById(postingId)
                .orElseThrow(() -> new JobBotException("Posting not found: " + postingId));

        int matchScore = 0;
        Optional<CandidateProfile> profile = profileService.currentProfile();
        if (profile.isPresent()) {
            MatchResult m = matchService.match(profile.get(), posting);
            matchScore = m.overallScore();
        }
        String variant = selectionService.selectBest(posting).variant().name();

        ManualQueueEntry entry = ManualQueueEntry.builder()
                .postingId(posting.getId())
                .company(posting.getCompany())
                .role(posting.getTitle())
                .source(posting.getSource() != null ? posting.getSource().name() : null)
                .jobUrl(posting.getSourceUrl())
                .applicationUrl(posting.getApplicationUrl())
                .capability(posting.getApplicationCapability() != null
                        ? posting.getApplicationCapability().name() : null)
                .reason(reasonFor(posting))
                .matchScore(matchScore)
                .recommendedVariant(variant)
                .status(ManualQueueStatus.PENDING)
                .build();
        ManualQueueEntry saved = repository.save(entry);
        log.info("Added posting '{}' @ {} to manual queue", saved.getRole(), saved.getCompany());
        return saved;
    }

    public List<ManualQueueEntry> list(ManualQueueStatus status) {
        return status != null
                ? repository.findByStatusOrderByMatchScoreDesc(status)
                : repository.findAllByOrderByMatchScoreDesc();
    }

    @Transactional
    public ManualQueueEntry markOpened(UUID id) {
        ManualQueueEntry e = get(id);
        if (e.getStatus() == ManualQueueStatus.PENDING) {
            e.setStatus(ManualQueueStatus.OPENED);
        }
        return repository.save(e);
    }

    /** User applied manually → carry into the Kanban CRM as a non-auto application (§27→§29). */
    @Transactional
    public ManualQueueEntry markApplied(UUID id) {
        ManualQueueEntry e = get(id);
        if (e.getStatus() == ManualQueueStatus.APPLIED) return e;

        Application app = applicationRepository.save(Application.builder()
                .company(e.getCompany())
                .title(e.getRole())
                .platform(e.getSource())
                .status("applied")
                .atsScore(BigDecimal.valueOf(e.getMatchScore()))
                .autoApplied(false)          // spec §1/§27: manual, never auto
                .jobQueueId(e.getId())
                .appliedAt(OffsetDateTime.now())
                .build());

        e.setStatus(ManualQueueStatus.APPLIED);
        e.setAppliedAt(OffsetDateTime.now());
        e.setApplicationId(app.getId());
        log.info("Manual apply recorded for '{}' @ {} → application {}", e.getRole(), e.getCompany(), app.getId());
        return repository.save(e);
    }

    @Transactional
    public ManualQueueEntry skip(UUID id) {
        ManualQueueEntry e = get(id);
        e.setStatus(ManualQueueStatus.SKIPPED);
        return repository.save(e);
    }

    public Map<String, Long> stats() {
        return Map.of(
                "pending", repository.countByStatus(ManualQueueStatus.PENDING),
                "opened", repository.countByStatus(ManualQueueStatus.OPENED),
                "applied", repository.countByStatus(ManualQueueStatus.APPLIED),
                "skipped", repository.countByStatus(ManualQueueStatus.SKIPPED));
    }

    private ManualQueueEntry get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Manual queue entry not found: " + id));
    }

    private String reasonFor(JobPosting p) {
        AtsType src = p.getSource();
        if (src == AtsType.LINKEDIN) {
            return "LinkedIn Easy Apply — handled by the Chrome Extension, not the server-side engine.";
        }
        if (p.getApplicationCapability() == null) {
            return "Manual submission required.";
        }
        return switch (p.getApplicationCapability()) {
            case ASSISTED_APPLY -> "Assisted: application pack prepared — submit on the company's application page.";
            case MANUAL_REQUIRED -> "No authorized submission integration available for this source.";
            case AUTO_ELIGIBLE -> "Auto-eligible via an authorized integration (user opt-in required).";
            case UNAVAILABLE -> "Posting is no longer available.";
        };
    }
}


