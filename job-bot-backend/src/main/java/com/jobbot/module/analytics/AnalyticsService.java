package com.jobbot.module.analytics;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.ApplicationCapability;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.manualqueue.ManualQueueRepository;
import com.jobbot.module.manualqueue.ManualQueueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analytics + learning engine (spec §30–31). Deterministic aggregation over
 * applications, discovered postings and the manual queue. The learning engine only
 * emits recommendations after enough data (≥20 applications) and never silently
 * changes user preferences — it returns "JobPilot Recommendation" suggestions.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Set<String> RESPONSE_STATUSES =
            Set.of("viewed", "shortlisted", "interview", "offer", "screening");
    /** Single source of truth — see {@link com.jobbot.common.JobPilotThresholds}. */
    private static final int LEARNING_MIN_APPLICATIONS =
            com.jobbot.common.JobPilotThresholds.LEARNING_MIN_APPLICATIONS;

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository postingRepository;
    private final ManualQueueRepository manualQueueRepository;

    /** High-level metrics for the dashboard (spec §30). */
    public Map<String, Object> overview() {
        List<Application> apps = applicationRepository.findAll();
        long total = apps.size();
        long responses = apps.stream().filter(a -> RESPONSE_STATUSES.contains(a.getStatus())).count();
        long interviews = apps.stream().filter(a -> "interview".equals(a.getStatus())).count();
        long offers = apps.stream().filter(a -> "offer".equals(a.getStatus())).count();
        long manualApplications = apps.stream().filter(a -> !a.isAutoApplied()).count();
        double avgAts = apps.stream().filter(a -> a.getAtsScore() != null)
                .mapToDouble(a -> a.getAtsScore().doubleValue()).average().orElse(0);

        long autoEligible = postingRepository.countByApplicationCapability(ApplicationCapability.ASSISTED_APPLY)
                + postingRepository.countByApplicationCapability(ApplicationCapability.AUTO_ELIGIBLE);
        Double avgMatch = postingRepository.averageMatchScore();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jobsDiscovered", postingRepository.count());
        m.put("discoveredLast24h", postingRepository.countByCreatedAtAfter(OffsetDateTime.now().minusHours(24)));
        m.put("jobsMatched", postingRepository.countByMatchScoreGreaterThanEqual(65));
        m.put("strongMatches", postingRepository.countByRecommendation("STRONG_APPLY"));
        m.put("inQueue", manualQueueRepository.countByStatus(ManualQueueStatus.PENDING));
        m.put("autoEligibleJobs", autoEligible);
        m.put("manualRequiredJobs", postingRepository.countByApplicationCapability(ApplicationCapability.MANUAL_REQUIRED));
        m.put("applications", total);
        m.put("manualApplications", manualApplications);
        m.put("responseRate", pct(responses, total));
        m.put("interviewRate", pct(interviews, total));
        m.put("offerRate", pct(offers, total));
        m.put("averageAts", round1(avgAts));
        m.put("averageMatch", avgMatch == null ? 0 : round1(avgMatch));
        return m;
    }

    /** Role effectiveness — applications bucketed by inferred role (spec §31). */
    public List<Map<String, Object>> rolePerformance() {
        Map<String, List<Application>> byRole = applicationRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> classifyRole(a.getTitle())));
        return performanceRows(byRole, "role");
    }

    /** Source effectiveness — discovered vs applied per source (spec §30–31). */
    public List<Map<String, Object>> sourcePerformance() {
        Map<String, List<Application>> byPlatform = applicationRepository.findAll().stream()
                .filter(a -> a.getPlatform() != null)
                .collect(Collectors.groupingBy(a -> a.getPlatform().toUpperCase(Locale.ROOT)));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (AtsType src : AtsType.values()) {
            long discovered = postingRepository.countBySource(src);
            List<Application> apps = byPlatform.getOrDefault(src.name(), List.of());
            if (discovered == 0 && apps.isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", src.name());
            row.put("discovered", discovered);
            row.put("applications", (long) apps.size());
            row.put("interviews", apps.stream().filter(a -> "interview".equals(a.getStatus())).count());
            row.put("responseRate", pct(apps.stream().filter(a -> RESPONSE_STATUSES.contains(a.getStatus())).count(), apps.size()));
            rows.add(row);
        }
        return rows;
    }

    /** Distribution of discovered postings by location (proxy for location coverage, §30). */
    public List<Map<String, Object>> locationPerformance() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : postingRepository.locationDistribution()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("location", r[0]);
            row.put("discovered", ((Number) r[1]).longValue());
            rows.add(row);
            if (rows.size() >= 12) break;
        }
        return rows;
    }

    /**
     * Learning recommendations (spec §31). Only after ≥20 applications. Returns
     * "JobPilot Recommendation" suggestions the user can accept or ignore — never
     * applied automatically.
     */
    public Map<String, Object> learning() {
        List<Application> apps = applicationRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applications", apps.size());
        result.put("threshold", LEARNING_MIN_APPLICATIONS);
        if (apps.size() < LEARNING_MIN_APPLICATIONS) {
            result.put("ready", false);
            result.put("message", "Collect more data — " + (LEARNING_MIN_APPLICATIONS - apps.size())
                    + " more applications needed before learning insights unlock.");
            result.put("recommendations", List.of());
            return result;
        }
        result.put("ready", true);
        List<String> recs = new ArrayList<>();
        bestBucket(rolePerformance(), "role").ifPresent(b ->
                recs.add("Increase priority of " + b + " roles — they convert best for you."));
        bestBucket(sourcePerformance(), "source").ifPresent(b ->
                recs.add("Focus discovery on " + b + " — highest response rate."));
        result.put("recommendations", recs);
        return result;
    }

    // ---- helpers ----

    private List<Map<String, Object>> performanceRows(Map<String, List<Application>> grouped, String key) {
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.forEach((bucket, apps) -> {
            long count = apps.size();
            long responses = apps.stream().filter(a -> RESPONSE_STATUSES.contains(a.getStatus())).count();
            long interviews = apps.stream().filter(a -> "interview".equals(a.getStatus())).count();
            long offers = apps.stream().filter(a -> "offer".equals(a.getStatus())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(key, bucket);
            row.put("applications", count);
            row.put("responses", responses);
            row.put("interviews", interviews);
            row.put("offers", offers);
            row.put("responseRate", pct(responses, count));
            row.put("interviewRate", pct(interviews, count));
            rows.add(row);
        });
        rows.sort((a, b) -> Double.compare((double) b.get("interviewRate"), (double) a.get("interviewRate")));
        return rows;
    }

    private java.util.Optional<String> bestBucket(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .filter(r -> ((Number) r.get("applications")).longValue() >= 3)
                .max((a, b) -> Double.compare((double) a.get("responseRate"), (double) b.get("responseRate")))
                .map(r -> String.valueOf(r.get(key)));
    }

    private String classifyRole(String title) {
        if (title == null) return "Other";
        String t = title.toLowerCase(Locale.ROOT);
        if (t.contains("full stack") || t.contains("fullstack")) return "Java Full Stack";
        if (t.contains("microservice") || t.contains("kafka")) return "Java Microservices";
        if (t.contains("aws") || t.contains("cloud")) return "Java Cloud";
        if (t.contains("backend") || t.contains("java")) return "Java Backend";
        return "Other";
    }

    private double pct(long num, long den) {
        return den == 0 ? 0 : Math.round(num * 1000.0 / den) / 10.0;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

