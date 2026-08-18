package com.jobbot.module.discovery;

import com.jobbot.module.company.Company;
import com.jobbot.module.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Source health + coverage stats. India-market posture:
 *  - NAUKRI / INDEED / LINKEDIN are ACTIVE search-based discovery sources.
 *  - LINKEDIN application is delegated to the Chrome Extension; server-side apply
 *    is not attempted. Discovery still runs.
 *  - GREENHOUSE / ASHBY etc. show only if the user has manually seeded a company.
 */
@Service
@RequiredArgsConstructor
public class SourceHealthService {

    private final CompanyRepository companyRepository;
    private final JobPostingRepository postingRepository;

    public record SourceHealthRow(String source, SourceStatus status, int companies,
                                  OffsetDateTime lastChecked) {}

    public record CoverageStats(int sourcesConfigured, int sourcesActive, int sourcesManual,
                                int sourcesUnavailable, long companiesMonitored,
                                long postingsTotal, long postingsLast24h) {}

    public List<SourceHealthRow> health() {
        List<Company> companies = companyRepository.findAllByActiveTrue();
        Map<AtsType, List<Company>> byAts = companies.stream()
                .collect(Collectors.groupingBy(Company::getAtsType));

        List<SourceHealthRow> rows = new ArrayList<>();

        // Primary India discovery sources — always shown as ACTIVE.
        rows.add(new SourceHealthRow("NAUKRI", SourceStatus.HEALTHY, 0, null));
        rows.add(new SourceHealthRow("LINKEDIN", SourceStatus.HEALTHY, 0, null));
        rows.add(new SourceHealthRow("INDEED", SourceStatus.HEALTHY, 0, null));

        // ATS integrations that require company seeding.
        for (AtsType ats : List.of(AtsType.GREENHOUSE, AtsType.ASHBY, AtsType.LEVER, AtsType.WORKABLE)) {
            List<Company> list = byAts.getOrDefault(ats, List.of());
            if (list.isEmpty()) continue;
            rows.add(new SourceHealthRow(
                    ats.name(),
                    aggregateStatus(list),
                    list.size(),
                    latestChecked(list)));
        }

        // Manual URL import.
        rows.add(new SourceHealthRow("MANUAL", SourceStatus.HEALTHY,
                byAts.getOrDefault(AtsType.MANUAL, List.of()).size(), null));
        return rows;
    }

    public CoverageStats coverage() {
        List<SourceHealthRow> rows = health();
        int configured = rows.size();
        int active = (int) rows.stream()
                .filter(r -> r.status() == SourceStatus.HEALTHY && !r.source().equals("MANUAL"))
                .count();
        int manual = (int) rows.stream().filter(r -> r.status() == SourceStatus.MANUAL).count();
        int unavailable = (int) rows.stream()
                .filter(r -> r.status() == SourceStatus.UNAVAILABLE).count();
        long postingsTotal = postingRepository.count();
        long last24h = postingRepository.countByCreatedAtAfter(OffsetDateTime.now().minusHours(24));
        return new CoverageStats(configured, active, manual, unavailable,
                companyRepository.countByActiveTrue(), postingsTotal, last24h);
    }

    private SourceStatus aggregateStatus(List<Company> companies) {
        boolean anyUnavailable = companies.stream()
                .anyMatch(c -> c.getSourceStatus() == SourceStatus.UNAVAILABLE);
        boolean anyDegraded = companies.stream()
                .anyMatch(c -> c.getSourceStatus() == SourceStatus.DEGRADED);
        if (anyUnavailable) return SourceStatus.DEGRADED;
        if (anyDegraded) return SourceStatus.DEGRADED;
        return SourceStatus.HEALTHY;
    }

    private OffsetDateTime latestChecked(List<Company> companies) {
        return companies.stream()
                .map(Company::getLastChecked)
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
    }
}
