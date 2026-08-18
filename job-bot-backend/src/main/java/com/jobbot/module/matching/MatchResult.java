package com.jobbot.module.matching;

import java.util.List;

/**
 * The full 8-factor match breakdown for one (candidate, job) pair (spec §14).
 * All sub-scores are 0–100. {@code overallScore} is the weighted combination.
 */
public record MatchResult(
        int overallScore,
        int technicalScore,
        int experienceScore,
        int roleScore,
        int locationScore,
        int workModeScore,
        int noticeScore,
        int salaryScore,
        int companyScore,
        List<String> matchedSkills,
        List<String> missingRequiredSkills,
        List<String> preferredSkillGaps,
        List<String> riskFactors,
        Recommendation recommendation
) {}

