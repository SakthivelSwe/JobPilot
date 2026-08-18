package com.jobbot.module.candidate.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The user-verified profile payload sent to POST /api/candidate/profile/confirm (spec §4).
 * Only what the user confirms is persisted — nothing is silently overwritten.
 */
public record ConfirmProfileDTO(
        String name,
        String email,
        String phone,
        String currentLocation,
        List<String> preferredLocations,
        List<String> preferredWorkModes,
        BigDecimal yearsOfExperience,
        Integer noticePeriodDays,
        String lastWorkingDate,          // ISO yyyy-MM-dd or null
        BigDecimal expectedSalary,
        BigDecimal minimumSalary,
        String relocationPreference,
        String remotePreference,
        String workAuthorization,
        String summary,
        List<String> targetRoles,
        List<String> excludedRoles,
        List<String> preferredCompanies,
        List<String> excludedCompanies,
        List<SkillInput> skills,
        List<ExperienceInput> experiences,
        List<ProjectInput> projects,
        List<EducationInput> education,
        List<CertificationInput> certifications,
        List<String> achievements,
        String storagePath,              // link the source doc that produced this
        String fileName,
        String mimeType,
        Long size,
        String checksum,
        String extractedText
) {
    public record SkillInput(String name, String category, String proficiency, List<String> evidence) {}
    public record ExperienceInput(String company, String role, String startDate, String endDate,
                                  boolean current, String description, List<String> technologies) {}
    public record ProjectInput(String name, String description, List<String> technologies) {}
    public record EducationInput(String institution, String degree, String field,
                                 Integer startYear, Integer endYear, String grade) {}
    public record CertificationInput(String name, String issuer, String issuedDate, String credentialId) {}
}

