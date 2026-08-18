package com.jobbot.module.discovery;

import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateProfileService;
import com.jobbot.module.company.Company;
import com.jobbot.module.company.CompanyService;
import com.jobbot.module.criteria.CriteriaRepository;
import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.discovery.adapter.DiscoveredPosting;
import com.jobbot.module.discovery.adapter.JobSourceAdapter;
import com.jobbot.module.discovery.adapter.SearchBasedAdapter;
import com.jobbot.module.matching.JobMatchService;
import com.jobbot.module.matching.MatchResult;
import com.jobbot.module.queue.JobQueueService;
import com.jobbot.module.role.TargetRole;
import com.jobbot.module.role.TargetRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discovery orchestrator.
 *
 * <p>Two flavours of adapter run per scan:
 * <ol>
 *   <li>Company-scoped ATS adapters ({@link JobSourceAdapter}) — currently only
 *       used if the user has manually seeded companies with GREENHOUSE/ASHBY tokens.</li>
 *   <li>Search-based adapters ({@link SearchBasedAdapter}) — Naukri / LinkedIn /
 *       Indeed — run once per active {@link TargetRole}, using the first active
 *       criteria for score threshold.</li>
 * </ol>
 *
 * <p>Every new posting is normalized, deduplicated, matched against the profile,
 * classified for capability and — when scoring above the criteria threshold —
 * auto-enqueued into the {@link JobQueueService} as PENDING_REVIEW.
 */
@Service
@Slf4j
public class JobDiscoveryService {

    private final CompanyService companyService;
    private final JobNormalizer normalizer;
    private final DeduplicationService dedup;
    private final ApplicationCapabilityService capability;
    private final JobPostingRepository postingRepository;
    private final CandidateProfileService profileService;
    private final JobMatchService matchService;
    private final TargetRoleRepository roleRepository;
    private final CriteriaRepository criteriaRepository;
    private final JobQueueService queueService;
    private final com.jobbot.module.activity.ActivityService activity;

    private final Map<AtsType, JobSourceAdapter> companyAdapters = new EnumMap<>(AtsType.class);
    private final Map<AtsType, SearchBasedAdapter> searchAdapters = new EnumMap<>(AtsType.class);

    public JobDiscoveryService(CompanyService companyService,
                               JobNormalizer normalizer,
                               DeduplicationService dedup,
                               ApplicationCapabilityService capability,
                               JobPostingRepository postingRepository,
                               CandidateProfileService profileService,
                               JobMatchService matchService,
                               TargetRoleRepository roleRepository,
                               CriteriaRepository criteriaRepository,
                               JobQueueService queueService,
                               com.jobbot.module.activity.ActivityService activity,
                               List<JobSourceAdapter> companyAdapterList,
                               List<SearchBasedAdapter> searchAdapterList) {
        this.companyService = companyService;
        this.normalizer = normalizer;
        this.dedup = dedup;
        this.capability = capability;
        this.postingRepository = postingRepository;
        this.profileService = profileService;
        this.matchService = matchService;
        this.roleRepository = roleRepository;
        this.criteriaRepository = criteriaRepository;
        this.queueService = queueService;
        this.activity = activity;
        for (JobSourceAdapter a : companyAdapterList) companyAdapters.put(a.type(), a);
        for (SearchBasedAdapter a : searchAdapterList) searchAdapters.put(a.type(), a);
    }

    public DiscoveryResult scan() {
        List<DiscoveryResult.SourceOutcome> outcomes = new ArrayList<>();
        int totalFound = 0, newPostings = 0, duplicates = 0, alreadySeen = 0, errors = 0, scanned = 0;

        Optional<CandidateProfile> profile = profileService.currentProfile();
        List<JobCriteria> activeCriteria = criteriaRepository.findAllByActiveTrue();
        JobCriteria primary = activeCriteria.isEmpty() ? null : activeCriteria.get(0);
        BigDecimal threshold = primary != null && primary.getMinMatchScore() != null
                ? primary.getMinMatchScore() : com.jobbot.common.JobPilotThresholds.DEFAULT_MIN_MATCH_SCORE_BD;

        // 1) Company-scoped adapters (only if user seeded ATS companies)
        for (Company company : companyService.getActive()) {
            JobSourceAdapter adapter = companyAdapters.get(company.getAtsType());
            if (adapter == null) continue;
            scanned++;
            try {
                List<DiscoveredPosting> raw = adapter.fetch(company);
                int added = ingest(raw, profile, threshold, company);
                totalFound += raw.size();
                newPostings += added;
                companyService.markChecked(company, SourceStatus.HEALTHY);
                outcomes.add(new DiscoveryResult.SourceOutcome(
                        company.getName(), company.getAtsType(), SourceStatus.HEALTHY,
                        raw.size(), added, null));
            } catch (Exception e) {
                errors++;
                companyService.markChecked(company, SourceStatus.UNAVAILABLE);
                outcomes.add(new DiscoveryResult.SourceOutcome(
                        company.getName(), company.getAtsType(), SourceStatus.UNAVAILABLE,
                        0, 0, e.getMessage()));
                log.warn("Discovery failed for '{}': {}", company.getName(), e.getMessage());
            }
        }

        // 2) Search-based adapters (Naukri / LinkedIn / Indeed) per active target role
        List<TargetRole> roles = roleRepository.findAllByActiveTrueOrderByPriorityAsc();
        for (TargetRole role : roles) {
            for (SearchBasedAdapter adapter : searchAdapters.values()) {
                scanned++;
                String label = role.getRoleTitle() + " · " + adapter.type().name();
                try {
                    List<DiscoveredPosting> raw = adapter.discover(role, primary);
                    int added = ingest(raw, profile, threshold, null);
                    totalFound += raw.size();
                    newPostings += added;
                    outcomes.add(new DiscoveryResult.SourceOutcome(
                            label, adapter.type(), SourceStatus.HEALTHY,
                            raw.size(), added, null));
                } catch (Exception e) {
                    errors++;
                    outcomes.add(new DiscoveryResult.SourceOutcome(
                            label, adapter.type(), SourceStatus.UNAVAILABLE,
                            0, 0, e.getMessage()));
                    log.warn("Search discovery failed for {}: {}", label, e.getMessage());
                }
            }
        }

        activity.record("DISCOVERY", "Discovery completed",
                newPostings + " new · " + totalFound + " found across " + scanned + " sources");
        return new DiscoveryResult(scanned, totalFound, newPostings, duplicates, alreadySeen,
                errors, OffsetDateTime.now(), outcomes);
    }

    /** Common ingestion pipeline: normalize → dedupe → classify → score → persist → enqueue. */
    private int ingest(List<DiscoveredPosting> raw,
                       Optional<CandidateProfile> profile,
                       BigDecimal threshold,
                       Company company) {
        int added = 0;
        for (DiscoveredPosting dp : raw) {
            JobPosting candidate = normalizer.normalize(dp);
            DeduplicationService.DedupResult r = dedup.check(candidate);
            switch (r.outcome()) {
                case NEW -> {
                    if (company != null) candidate.setCompanyId(company.getId());
                    candidate.setApplicationCapability(capability.determine(candidate.getSource()));
                    if (profile.isPresent()) {
                        try {
                            MatchResult m = matchService.match(profile.get(), candidate);
                            candidate.setMatchScore(m.overallScore());
                            candidate.setRecommendation(m.recommendation().name());
                        } catch (Exception ignored) { /* score best-effort */ }
                    }
                    JobPosting saved = postingRepository.save(candidate);
                    try {
                        queueService.enqueueFromPosting(saved, threshold);
                    } catch (Exception e) {
                        log.warn("Enqueue failed for '{}': {}", saved.getTitle(), e.getMessage());
                    }
                    added++;
                }
                case CROSS_SOURCE_DUPLICATE -> dedup.mergeSourceHistory(r.existing(), candidate);
                case ALREADY_SEEN -> { /* skip */ }
            }
        }
        return added;
    }
}

