package com.jobbot.module.resume.variant;

import com.jobbot.module.discovery.JobPosting;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Automatic resume-variant selection (spec §20): analyze a job, rank the four
 * variants by how well their priority skills + role target match the job, and flag
 * the best one.
 */
@Service
public class ResumeSelectionService {

    /** Rank all variants against a job, best first, with the top flagged recommended. */
    public List<VariantScore> rank(JobPosting job) {
        String haystack = haystack(job);
        List<VariantScore> scored = new ArrayList<>();
        for (ResumeVariant v : ResumeVariant.values()) {
            List<String> matched = new ArrayList<>();
            for (String skill : v.prioritySkills()) {
                if (containsWord(haystack, skill)) matched.add(skill);
            }
            int skillPct = v.prioritySkills().isEmpty() ? 0
                    : (int) Math.round(100.0 * matched.size() / v.prioritySkills().size());
            int roleBonus = roleOverlap(v.roleTarget(), job.getTitle());
            int score = (int) Math.round(skillPct * 0.8 + roleBonus * 0.2);
            scored.add(new VariantScore(v, v.roleTarget(), score, matched, false));
        }
        scored.sort(Comparator.comparingInt(VariantScore::score).reversed());
        if (!scored.isEmpty()) {
            VariantScore top = scored.get(0);
            scored.set(0, new VariantScore(top.variant(), top.roleTarget(), top.score(),
                    top.matchedPrioritySkills(), true));
        }
        return scored;
    }

    public VariantScore selectBest(JobPosting job) {
        return rank(job).get(0);
    }

    // ---- helpers ----

    private int roleOverlap(String roleTarget, String title) {
        if (title == null) return 0;
        var roleTokens = tokens(roleTarget);
        var titleTokens = tokens(title);
        if (roleTokens.isEmpty()) return 0;
        long overlap = roleTokens.stream().filter(titleTokens::contains).count();
        return (int) Math.round(100.0 * overlap / roleTokens.size());
    }

    private String haystack(JobPosting job) {
        StringBuilder sb = new StringBuilder();
        if (job.getTitle() != null) sb.append(job.getTitle()).append(' ');
        if (job.getDescription() != null) sb.append(job.getDescription()).append(' ');
        if (job.getRequiredSkills() != null) sb.append(String.join(" ", job.getRequiredSkills())).append(' ');
        if (job.getPreferredSkills() != null) sb.append(String.join(" ", job.getPreferredSkills()));
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private boolean containsWord(String haystackLower, String term) {
        if (term == null || term.isBlank()) return false;
        return Pattern.compile("\\b" + Pattern.quote(term.toLowerCase(Locale.ROOT).trim()) + "\\b")
                .matcher(haystackLower).find();
    }

    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");

    private java.util.Set<String> tokens(String s) {
        if (s == null) return java.util.Set.of();
        return java.util.Arrays.stream(NON_WORD.split(s.toLowerCase(Locale.ROOT)))
                .filter(t -> t.length() > 1)
                .filter(t -> !STOP.contains(t))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static final java.util.Set<String> STOP = java.util.Set.of(
            "developer", "engineer", "senior", "junior", "the", "and", "for");
}

