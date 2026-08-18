package com.jobbot.module.search;

import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.resume.ResumeRepository;
import com.jobbot.module.role.TargetRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic global search across the user's real data (rule 54/67).
 * Case-insensitive substring match over already-loaded rows — no fabricated
 * suggestions, no external calls. Results carry a route so the UI can navigate.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    public record SearchHit(String kind, String title, String subtitle, String route) {}
    public record SearchResults(String query, int total, List<SearchHit> hits) {}

    private static final int PER_KIND = 6;

    private final JobPostingRepository postingRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final TargetRoleRepository roleRepository;

    public SearchResults search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<SearchHit> hits = new ArrayList<>();
        if (q.length() < 2) return new SearchResults(query, 0, hits);

        // Discovered postings (opportunities)
        postingRepository.findAll().stream()
                .filter(p -> contains(p.getTitle(), q) || contains(p.getCompany(), q) || contains(p.getLocation(), q))
                .limit(PER_KIND)
                .forEach(p -> hits.add(new SearchHit("Opportunity",
                        p.getTitle(), companyLine(p.getCompany(), p.getLocation()),
                        "/jobs/posting/" + p.getId())));

        // Applications (pipeline)
        applicationRepository.findAll().stream()
                .filter(a -> contains(a.getCompany(), q) || contains(a.getTitle(), q))
                .limit(PER_KIND)
                .forEach(a -> hits.add(new SearchHit("Application",
                        a.getTitle() != null ? a.getTitle() : "Application",
                        companyLine(a.getCompany(), a.getStatus()), "/applications")));

        // Résumés (library)
        resumeRepository.findAll().stream()
                .filter(r -> contains(r.getName(), q)
                        || (r.getTargetSkills() != null && r.getTargetSkills().stream().anyMatch(s -> contains(s, q))))
                .limit(PER_KIND)
                .forEach(r -> hits.add(new SearchHit("Résumé",
                        r.getName(), "Résumé library", "/resumes/" + r.getId())));

        // Target roles (strategy)
        roleRepository.findAll().stream()
                .filter(t -> contains(t.getRoleTitle(), q)
                        || (t.getRequiredSkills() != null && t.getRequiredSkills().stream().anyMatch(s -> contains(s, q))))
                .limit(PER_KIND)
                .forEach(t -> hits.add(new SearchHit("Target role",
                        t.getRoleTitle(), "Search strategy", "/target-roles")));

        // Companies (distinct names from postings + applications)
        java.util.LinkedHashSet<String> companies = new java.util.LinkedHashSet<>();
        postingRepository.findAll().forEach(p -> { if (contains(p.getCompany(), q)) companies.add(p.getCompany()); });
        applicationRepository.findAll().forEach(a -> { if (contains(a.getCompany(), q)) companies.add(a.getCompany()); });
        companies.stream().limit(PER_KIND).forEach(name -> hits.add(new SearchHit("Company",
                name, "Company workspace", "/companies/" + encode(name))));

        return new SearchResults(query, hits.size(), hits);
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String companyLine(String a, String b) {
        if (a == null && b == null) return "";
        if (a == null) return b;
        if (b == null) return a;
        return a + " · " + b;
    }
}

