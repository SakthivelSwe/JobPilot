package com.jobbot.module.candidate;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.candidate.dto.ConfirmProfileDTO;
import com.jobbot.module.candidate.dto.ParsedResumeDTO;
import com.jobbot.module.candidate.parse.ResumeExtractionService;
import com.jobbot.module.candidate.parse.ResumeParser;
import com.jobbot.module.candidate.parse.ResumeValidationService;
import com.jobbot.module.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 1 candidate-profile service: parse an uploaded resume into an unsaved preview,
 * then persist a user-verified profile. Never silently overwrites verified data (spec §4).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepo;
    private final ResumeSourceDocumentRepository docRepo;
    private final ResumeValidationService validation;
    private final ResumeParser parser;
    private final ResumeExtractionService extraction;
    private final StorageService storage;

    /** Parse an uploaded resume and return an UNSAVED extraction preview (spec §4). */
    public ParsedResumeDTO parse(String fileName, String mimeType, byte[] bytes) {
        long size = bytes == null ? 0 : bytes.length;
        validation.validate(fileName, size, bytes);
        String text = parser.extractText(fileName, mimeType, bytes);
        String checksum = sha256(bytes);
        String storagePath = storage.store("resumes", fileName, bytes);
        log.info("Parsed resume '{}' ({} bytes) → stored at {}", fileName, size, storagePath);
        return extraction.extract(fileName, mimeType, size, checksum, storagePath, text);
    }

    /** Persist (create or update) the user-verified profile (spec §4). */
    @Transactional
    public CandidateProfile confirm(ConfirmProfileDTO dto) {
        if (dto == null) throw new JobBotException("Profile payload required");

        CandidateProfile p = currentProfile().orElseGet(CandidateProfile::new);

        p.setName(dto.name());
        p.setEmail(dto.email());
        p.setPhone(dto.phone());
        p.setCurrentLocation(dto.currentLocation());
        if (dto.preferredLocations() != null) p.setPreferredLocations(dto.preferredLocations());
        if (dto.preferredWorkModes() != null) p.setPreferredWorkModes(dto.preferredWorkModes());
        p.setYearsOfExperience(dto.yearsOfExperience());
        p.setNoticePeriodDays(dto.noticePeriodDays());
        p.setLastWorkingDate(parseDate(dto.lastWorkingDate()));
        p.setExpectedSalary(dto.expectedSalary());
        p.setMinimumSalary(dto.minimumSalary());
        p.setRelocationPreference(dto.relocationPreference());
        p.setRemotePreference(dto.remotePreference());
        p.setWorkAuthorization(dto.workAuthorization());
        p.setSummary(dto.summary());
        if (dto.targetRoles() != null) p.setTargetRoles(dto.targetRoles());
        if (dto.excludedRoles() != null) p.setExcludedRoles(dto.excludedRoles());
        if (dto.preferredCompanies() != null) p.setPreferredCompanies(dto.preferredCompanies());
        if (dto.excludedCompanies() != null) p.setExcludedCompanies(dto.excludedCompanies());
        p.setVerified(true);

        if (p.getSkills() == null) p.setSkills(new java.util.ArrayList<>());
        p.getSkills().clear();
        if (dto.skills() != null) {
            for (var s : dto.skills()) {
                CandidateSkill skill = CandidateSkill.builder()
                        .name(s.name())
                        .category(s.category())
                        .proficiency(safeProficiency(s.proficiency()))
                        .userVerified(true)
                        .build();
                if (s.evidence() != null) {
                    for (String ev : s.evidence()) {
                        skill.addEvidence(SkillEvidence.builder()
                                .type("USER").description(ev).build());
                    }
                }
                p.addSkill(skill);
            }
        }

        if (p.getExperiences() == null) p.setExperiences(new java.util.ArrayList<>());
        p.getExperiences().clear();
        if (dto.experiences() != null) {
            for (var e : dto.experiences()) {
                p.addExperience(WorkExperience.builder()
                        .company(e.company()).role(e.role())
                        .startDate(parseDate(e.startDate())).endDate(parseDate(e.endDate()))
                        .current(e.current()).description(e.description())
                        .technologies(e.technologies() == null ? List.of() : e.technologies())
                        .build());
            }
        }

        if (p.getProjects() == null) p.setProjects(new java.util.ArrayList<>());
        p.getProjects().clear();
        if (dto.projects() != null) {
            for (var pr : dto.projects()) {
                p.addProject(Project.builder()
                        .name(pr.name()).description(pr.description())
                        .technologies(pr.technologies() == null ? List.of() : pr.technologies())
                        .build());
            }
        }

        if (p.getEducation() == null) p.setEducation(new java.util.ArrayList<>());
        p.getEducation().clear();
        if (dto.education() != null) {
            for (var ed : dto.education()) {
                p.addEducation(Education.builder()
                        .institution(ed.institution()).degree(ed.degree()).field(ed.field())
                        .startYear(ed.startYear()).endYear(ed.endYear()).grade(ed.grade())
                        .build());
            }
        }

        if (p.getCertifications() == null) p.setCertifications(new java.util.ArrayList<>());
        p.getCertifications().clear();
        if (dto.certifications() != null) {
            for (var c : dto.certifications()) {
                p.addCertification(Certification.builder()
                        .name(c.name()).issuer(c.issuer())
                        .issuedDate(parseDate(c.issuedDate())).credentialId(c.credentialId())
                        .build());
            }
        }

        if (p.getAchievements() == null) p.setAchievements(new java.util.ArrayList<>());
        p.getAchievements().clear();
        if (dto.achievements() != null) {
            for (String a : dto.achievements()) {
                p.addAchievement(Achievement.builder().description(a).build());
            }
        }

        CandidateProfile saved = profileRepo.save(p);

        // Persist source-document metadata (spec §51 — metadata only).
        if (dto.storagePath() != null && dto.fileName() != null) {
            docRepo.save(ResumeSourceDocument.builder()
                    .profileId(saved.getId())
                    .fileName(dto.fileName())
                    .mimeType(dto.mimeType())
                    .size(dto.size() == null ? 0 : dto.size())
                    .checksum(dto.checksum())
                    .storagePath(dto.storagePath())
                    .extractedText(dto.extractedText())
                    .build());
        }

        log.info("Confirmed candidate profile {} ({} skills, {} experiences)",
                saved.getId(), saved.getSkills().size(), saved.getExperiences().size());
        return saved;
    }

    public Optional<CandidateProfile> currentProfile() {
        // Single-user app: the most recently updated profile is "the" profile.
        return profileRepo.findAll().stream()
                .max((a, b) -> a.getUpdatedAt().compareTo(b.getUpdatedAt()));
    }

    public CandidateProfile getOrThrow() {
        return currentProfile().orElseThrow(() -> new JobBotException("No candidate profile yet"));
    }

    public List<CandidateSkill> skills() {
        return getOrThrow().getSkills();
    }

    // --- helpers ---

    private Proficiency safeProficiency(String raw) {
        if (raw == null) return Proficiency.UNKNOWN;
        try {
            return Proficiency.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Proficiency.UNKNOWN;
        }
    }

    private LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDate.parse(iso.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}

