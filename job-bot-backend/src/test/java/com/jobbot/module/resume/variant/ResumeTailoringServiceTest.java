package com.jobbot.module.resume.variant;

import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateSkill;
import com.jobbot.module.candidate.Project;
import com.jobbot.module.candidate.WorkExperience;
import com.jobbot.module.discovery.JobPosting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ResumeTailoringServiceTest {

    private final ResumeTailoringService service = new ResumeTailoringService();

    private CandidateProfile profile() {
        return CandidateProfile.builder()
                .yearsOfExperience(BigDecimal.valueOf(2.8))
                .summary("Java engineer building resilient services.")
                .skills(List.of(
                        CandidateSkill.builder().name("Java").build(),
                        CandidateSkill.builder().name("Spring Boot").build(),
                        CandidateSkill.builder().name("Kafka").build(),
                        CandidateSkill.builder().name("Angular").build()))
                .projects(List.of(
                        Project.builder().name("Payments").technologies(List.of("Java", "Kafka")).build(),
                        Project.builder().name("Portal").technologies(List.of("Angular")).build()))
                .experiences(List.of(
                        WorkExperience.builder().company("Acme").role("Backend Dev")
                                .current(true).technologies(List.of("Java", "Spring Boot", "Kafka")).build()))
                .build();
    }

    private JobPosting microservicesJob() {
        return JobPosting.builder()
                .title("Java Microservices Engineer")
                .requiredSkills(List.of("Java", "Kafka", "Microservices"))
                .description("Kafka-based microservices in Java Spring Boot.")
                .build();
    }

    @Test
    void tailoredSkillsAreNeverFabricated() {
        CandidateProfile p = profile();
        Set<String> profileSkills = p.getSkills().stream()
                .map(CandidateSkill::getName).collect(Collectors.toSet());

        TailoredResume t = service.tailor(p, microservicesJob(), ResumeVariant.JAVA_MICROSERVICES);

        // Every tailored skill must exist on the master profile (spec §21).
        assertTrue(profileSkills.containsAll(t.orderedSkills()));
        assertEquals(profileSkills.size(), t.orderedSkills().size());
    }

    @Test
    void jobRelevantSkillsAreEmphasizedFirst() {
        TailoredResume t = service.tailor(profile(), microservicesJob(), ResumeVariant.JAVA_MICROSERVICES);
        // Java and Kafka are both job-relevant + variant-priority → emphasized.
        assertTrue(t.emphasizedKeywords().contains("Java"));
        assertTrue(t.emphasizedKeywords().contains("Kafka"));
        // Angular is neither → not emphasized, and ordered after the relevant ones.
        assertFalse(t.emphasizedKeywords().contains("Angular"));
        assertTrue(t.orderedSkills().indexOf("Kafka") < t.orderedSkills().indexOf("Angular"));
    }

    @Test
    void summaryUsesRealYearsAndRoleTarget() {
        TailoredResume t = service.tailor(profile(), microservicesJob(), ResumeVariant.JAVA_MICROSERVICES);
        assertTrue(t.summary().contains("2.8 years"));
        assertTrue(t.summary().contains("Java Microservices Developer"));
        // Keeps the candidate's own base summary.
        assertTrue(t.summary().contains("Java engineer building resilient services."));
    }

    @Test
    void mostRelevantProjectRanksFirst() {
        TailoredResume t = service.tailor(profile(), microservicesJob(), ResumeVariant.JAVA_MICROSERVICES);
        assertEquals("Payments", t.projects().get(0).name()); // Java+Kafka > Angular
    }
}

