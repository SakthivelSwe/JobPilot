package com.jobbot.module.resume.variant;

import java.util.List;

/** A variant scored against a job (spec §20). */
public record VariantScore(
        ResumeVariant variant,
        String roleTarget,
        int score,
        List<String> matchedPrioritySkills,
        boolean recommended
) {}

