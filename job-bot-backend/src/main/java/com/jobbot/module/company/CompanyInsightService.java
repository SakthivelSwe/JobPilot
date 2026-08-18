package com.jobbot.module.company;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.manualqueue.ManualQueueEntry;
import com.jobbot.module.manualqueue.ManualQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Company-centric aggregation (rule 34). Groups the user's REAL data — discovered
 * postings, applications and saved (manual-queue) items — by company name. No
 * fabricated data; everything is read from persisted rows.
 */
@Service
@RequiredArgsConstructor
public class CompanyInsightService {

    private static final Set<String> INTERVIEW_STATUSES = Set.of("interview", "offer");

    private final JobPostingRepository postingRepository;
    private final ApplicationRepository applicationRepository;
    private final ManualQueueRepository manualQueueRepository;

    public record RoleRef(String id, String title, String location, Integer matchScore, String route) {}
    public record AppRef(String id, String title, String status, String appliedAt) {}
    public record CompanyOverview(
            String company,
            int openRoles, int applications, int interviews, int saved,
            List<RoleRef> roles, List<AppRef> apps) {}

    public CompanyOverview overview(String name) {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

        List<RoleRef> roles = postingRepository.findAll().stream()
                .filter(p -> matches(p.getCompany(), key))
                .map(p -> new RoleRef(p.getId().toString(), p.getTitle(), p.getLocation(),
                        p.getMatchScore(), "/jobs/posting/" + p.getId()))
                .toList();

        List<Application> apps = applicationRepository.findAll().stream()
                .filter(a -> matches(a.getCompany(), key))
                .toList();

        long interviews = apps.stream()
                .filter(a -> a.getStatus() != null && INTERVIEW_STATUSES.contains(a.getStatus().toLowerCase(Locale.ROOT)))
                .count();

        List<AppRef> appRefs = apps.stream()
                .map(a -> new AppRef(a.getId().toString(), a.getTitle(), a.getStatus(),
                        a.getAppliedAt() != null ? a.getAppliedAt().toString() : null))
                .toList();

        long saved = manualQueueRepository.findAll().stream()
                .filter(m -> matches(m.getCompany(), key))
                .count();

        return new CompanyOverview(name, roles.size(), apps.size(), (int) interviews, (int) saved, roles, appRefs);
    }

    private static boolean matches(String company, String key) {
        return company != null && company.toLowerCase(Locale.ROOT).equals(key);
    }
}

