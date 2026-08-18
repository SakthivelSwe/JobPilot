package com.jobbot.module.matching;

import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateSkill;
import com.jobbot.module.discovery.JobPosting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobMatchServiceTest {

    private final JobMatchService service = new JobMatchService(
            new LocationEngine(), new NoticePeriodEngine(), new RecommendationEngine());

    private CandidateProfile profile(String... skills) {
        return CandidateProfile.builder()
                .yearsOfExperience(BigDecimal.valueOf(2.8))
                .noticePeriodDays(60)
                .preferredLocations(List.of("Chennai", "Bengaluru"))
                .preferredWorkModes(List.of("HYBRID", "REMOTE"))
                .targetRoles(List.of("Java Backend Developer"))
                .skills(java.util.Arrays.stream(skills)
                        .map(s -> CandidateSkill.builder().name(s).build()).toList())
                .build();
    }

    private JobPosting.JobPostingBuilder baseJob() {
        return JobPosting.builder()
                .title("Senior Java Backend Engineer")
                .company("Acme").location("Bengaluru").remoteType("HYBRID")
                .requiredSkills(List.of("Java", "Spring Boot"))
                .preferredSkills(List.of("Kafka", "AWS"))
                .description("Build Java Spring Boot microservices. 2-4 years experience.")
                .minimumExperience(2).maximumExperience(4);
    }

    @Test
    void strongMatchProducesHighScoreAndStrongApply() {
        MatchResult r = service.match(profile("Java", "Spring Boot"), baseJob().build());
        assertEquals(100, r.technicalScore());
        assertEquals(100, r.experienceScore());
        assertEquals(100, r.roleScore());
        assertEquals(100, r.locationScore());
        assertTrue(r.overallScore() >= 90, "overall should be strong, was " + r.overallScore());
        assertEquals(Recommendation.STRONG_APPLY, r.recommendation());
        assertTrue(r.missingRequiredSkills().isEmpty());
        assertEquals(List.of("Kafka", "AWS"), r.preferredSkillGaps());
        assertTrue(r.matchedSkills().contains("Java"));
    }

    @Test
    void missingRequiredSkillsAreReported() {
        MatchResult r = service.match(profile("Python"), baseJob().build());
        assertEquals(List.of("Java", "Spring Boot"), r.missingRequiredSkills());
        assertEquals(0, r.technicalScore());
        assertTrue(r.riskFactors().stream().anyMatch(f -> f.contains("Missing required skills")));
    }

    @Test
    void excludedCompanyForcesSkip() {
        CandidateProfile p = profile("Java", "Spring Boot");
        p.setExcludedCompanies(List.of("Acme"));
        MatchResult r = service.match(p, baseJob().build());
        assertEquals(0, r.companyScore());
        assertEquals(Recommendation.SKIP, r.recommendation());
        assertTrue(r.riskFactors().contains("Excluded company"));
    }

    @Test
    void seniorRoleFarAboveExperienceIsHardMismatch() {
        JobPosting job = baseJob().minimumExperience(8).maximumExperience(12)
                .description("Java Spring Boot. 8-12 years experience.").build();
        MatchResult r = service.match(profile("Java", "Spring Boot"), job);
        assertTrue(r.experienceScore() < 60);
        assertEquals(Recommendation.LOW_PRIORITY, r.recommendation());
        assertTrue(r.riskFactors().stream().anyMatch(f -> f.contains("Experience")));
    }

    @Test
    void perfectExperienceRangeScoresFull() {
        assertEquals(100, service.experienceScore(2.8, 2, 4));
        assertEquals(100, service.experienceScore(2.8, 2, 5));
    }

    @Test
    void belowMinExperienceIsPenalized() {
        assertTrue(service.experienceScore(2.8, 6, 10) < 60);
    }

    @Test
    void unknownJobExperienceIsNeutral() {
        assertEquals(70, service.experienceScore(2.8, null, null));
    }
}


