package com.jobbot.module.candidate.dto;

import java.util.List;

/**
 * The result of parsing a resume — an UNSAVED preview the user must verify (spec §4).
 * "detected*" fields are what the parser found; the user confirms/edits before saving.
 */
public record ParsedResumeDTO(
        String fileName,
        String mimeType,
        long size,
        String checksum,
        String storagePath,
        String detectedName,
        String detectedEmail,
        String detectedPhone,
        String detectedSummary,
        List<DetectedSkillDTO> detectedSkills,
        List<DetectedExperienceDTO> detectedExperience,
        List<DetectedEducationDTO> detectedEducation,
        List<String> detectedProjects,
        String rawTextPreview
) {
    public record DetectedSkillDTO(String name, String category, String proficiency, List<String> evidence) {}
    public record DetectedExperienceDTO(String company, String role, String startDate, String endDate,
                                        boolean current, List<String> technologies) {}
    public record DetectedEducationDTO(String institution, String degree, String field,
                                       Integer startYear, Integer endYear) {}
}

