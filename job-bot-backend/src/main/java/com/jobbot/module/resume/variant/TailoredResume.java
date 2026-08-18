package com.jobbot.module.resume.variant;

import java.util.List;
import java.util.UUID;

/**
 * A tailored resume assembled ONLY from verified candidate facts (spec §21).
 * Tailoring reorders and emphasizes — it never invents skills, years, projects,
 * metrics or employment.
 */
public record TailoredResume(
        UUID profileId,
        ResumeVariant variant,
        String roleTarget,
        String title,
        String summary,
        List<String> orderedSkills,
        List<TailoredProject> projects,
        List<TailoredExperience> experiences,
        List<String> emphasizedKeywords
) {
    public record TailoredProject(String name, List<String> technologies) {}
    public record TailoredExperience(String company, String role, boolean current,
                                     List<String> technologies) {}
}

