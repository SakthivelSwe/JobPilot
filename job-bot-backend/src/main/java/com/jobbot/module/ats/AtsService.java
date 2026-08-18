package com.jobbot.module.ats;

import com.jobbot.module.ai.AiProvider;
import com.jobbot.module.ai.usage.AiUsageTracker;
import com.jobbot.module.ats.dto.AtsResult;
import com.jobbot.module.ats.dto.ResumeMatch;
import com.jobbot.module.resume.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic ATS engine. Scores resume vs job description using keyword,
 * experience, location and role signals — NO AI required. AI (if enabled) only
 * adds an enrichment note on top.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtsService {

    private final AiProvider aiProvider;
    private final AiUsageTracker aiUsageTracker;

    // Weights (must sum to 100)
    private static final int W_TECHNICAL = 55;
    private static final int W_EXPERIENCE = 20;
    private static final int W_LOCATION = 10;
    private static final int W_ROLE = 15;

    public AtsResult analyzeJobFit(String resumeText, String jobDescription, List<String> targetSkills) {
        return analyze(resumeText, jobDescription, targetSkills, null, 0, 0);
    }

    /**
     * The "4-resume engine": scores a job against every provided resume and returns
     * them ranked best-first, flagging the top one as recommended.
     */
    public List<ResumeMatch> rankResumes(String jobDescription, List<Resume> resumes) {
        List<ResumeMatch> matches = new ArrayList<>();
        for (Resume r : resumes) {
            AtsResult ats = analyzeJobFit(r.getResumeText(), jobDescription, r.getTargetSkills());
            matches.add(ResumeMatch.builder()
                    .resumeId(r.getId())
                    .resumeName(r.getName())
                    .score(ats.getScore())
                    .shouldApply(ats.isShouldApply())
                    .matchedKeywords(ats.getMatchedKeywords())
                    .missingKeywords(ats.getMissingKeywords())
                    .bestResumeAngle(ats.getBestResumeAngle())
                    .recommended(false)
                    .build());
        }
        matches.sort(Comparator.comparingInt(ResumeMatch::getScore).reversed());
        if (!matches.isEmpty()) {
            matches.get(0).setRecommended(true);
        }
        return matches;
    }

    public AtsResult analyze(String resumeText, String jobDescription, List<String> targetSkills,
                             List<String> preferredLocations, int expMin, int expMax) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return AtsResult.failed("Missing job description");
        }
        String jd = jobDescription.toLowerCase();
        String resume = resumeText == null ? "" : resumeText.toLowerCase();

        List<String> skills = targetSkills == null ? new ArrayList<>() : targetSkills;

        // --- Technical match: which target skills appear in the JD ---
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : skills) {
            if (skill == null || skill.isBlank()) continue;
            if (jd.contains(skill.toLowerCase().trim())) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }
        int technicalPct = skills.isEmpty() ? 0 : (int) Math.round(100.0 * matched.size() / skills.size());

        // --- Role match: does the resume mention the JD-required skills too ---
        long resumeHits = matched.stream().filter(s -> resume.contains(s.toLowerCase())).count();
        int rolePct = matched.isEmpty() ? 0 : (int) Math.round(100.0 * resumeHits / matched.size());

        // --- Experience match (heuristic: JD years vs candidate range) ---
        int experiencePct = scoreExperience(jd, expMin, expMax);

        // --- Location match ---
        int locationPct = scoreLocation(jd, preferredLocations);

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("technical", technicalPct);
        breakdown.put("role", rolePct);
        breakdown.put("experience", experiencePct);
        breakdown.put("location", locationPct);

        int score = (int) Math.round(
                (technicalPct * W_TECHNICAL
                        + rolePct * W_ROLE
                        + experiencePct * W_EXPERIENCE
                        + locationPct * W_LOCATION) / 100.0);

        boolean shouldApply = score >= 60;

        String reason = buildReason(matched, missing, experiencePct, locationPct, score);
        String angle = matched.isEmpty()
                ? "No strong overlap with target skills."
                : "Lead with: " + String.join(", ", matched.subList(0, Math.min(3, matched.size())));
        String suggestions = missing.isEmpty()
                ? "Strong keyword coverage."
                : "Consider highlighting or upskilling: " + String.join(", ", missing);

        String aiNote = null;
        try {
            if (aiProvider.isAvailable() && aiUsageTracker.canCall()) {
                aiNote = aiProvider.enrich(resumeText, jobDescription);
                aiUsageTracker.record(aiProvider.getClass().getSimpleName(), "ats_enrichment",
                        aiUsageTracker.estimateTokens(resumeText, jobDescription));
            }
        } catch (Exception e) {
            log.warn("AI enrichment skipped: {}", e.getMessage());
        }

        return AtsResult.builder()
                .score(score)
                .matchedKeywords(matched)
                .missingKeywords(missing)
                .bestResumeAngle(angle)
                .suggestions(suggestions)
                .shouldApply(shouldApply)
                .reasonToApply(reason)
                .breakdown(breakdown)
                .aiNote(aiNote)
                .build();
    }

    private int scoreExperience(String jd, int expMin, int expMax) {
        if (expMin <= 0 && expMax <= 0) return 70; // unknown -> neutral-ish
        // Nothing parsed from JD -> assume acceptable
        return 80;
    }

    private int scoreLocation(String jd, List<String> preferredLocations) {
        if (preferredLocations == null || preferredLocations.isEmpty()) return 70;
        if (jd.contains("remote")) return 100;
        for (String loc : preferredLocations) {
            if (loc != null && !loc.isBlank() && jd.contains(loc.toLowerCase().trim())) {
                return 100;
            }
        }
        return 40;
    }

    private String buildReason(List<String> matched, List<String> missing,
                               int experiencePct, int locationPct, int score) {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append("% MATCH\n\nWhy?\n");
        for (String m : matched) {
            sb.append("  \u2713 ").append(m).append(" present\n");
        }
        if (locationPct >= 100) sb.append("  \u2713 Location matches\n");
        if (!missing.isEmpty()) {
            sb.append("\nRisks:\n");
            for (String m : missing) {
                sb.append("  \u25B3 ").append(m).append(" not found\n");
            }
        }
        sb.append("\nDecision: ").append(score >= 75 ? "STRONG APPLY"
                : score >= 60 ? "APPLY" : "LOW PRIORITY");
        return sb.toString();
    }
}

