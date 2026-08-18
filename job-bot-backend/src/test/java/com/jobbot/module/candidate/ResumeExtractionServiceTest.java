package com.jobbot.module.candidate;

import com.jobbot.module.candidate.dto.ParsedResumeDTO;
import com.jobbot.module.candidate.parse.ResumeExtractionService;
import com.jobbot.module.candidate.parse.SkillNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 tests: skill normalization + deterministic extraction. No Spring context.
 */
class ResumeExtractionServiceTest {

    private final SkillNormalizer normalizer = new SkillNormalizer();
    private final ResumeExtractionService extraction = new ResumeExtractionService(normalizer);

    private static final String SAMPLE = """
            Sakthi Kumar
            sakthi.kumar@example.com | +91 98765 43210 | Chennai

            Summary: Java backend engineer with 2.8 years building Spring Boot microservices.

            Experience
            Software Engineer at Acme Corp  Jan 2022 - Present
            Built REST APIs with Spring Boot, Kafka and AWS.

            Education
            B.Tech Computer Science 2021

            Skills: Java, Spring Boot, Kafka, AWS, Angular, Kubernetes
            """;

    @Test
    void normalizesAliasesToCanonicalSkills() {
        assertEquals("Spring Boot", normalizer.normalize("springboot").canonical());
        assertEquals("Kafka", normalizer.normalize("Apache Kafka").canonical());
        assertEquals("AWS", normalizer.normalize("Amazon Web Services").canonical());
        assertEquals("Kubernetes", normalizer.normalize("k8s").canonical());
        assertNull(normalizer.normalize("nonexistent-tech"));
    }

    @Test
    void extractsCoreContactFields() {
        ParsedResumeDTO p = extraction.extract(
                "resume.txt", "text/plain", SAMPLE.length(), "abc", "resumes/x", SAMPLE);

        assertEquals("sakthi.kumar@example.com", p.detectedEmail());
        assertNotNull(p.detectedPhone());
        assertTrue(p.detectedName() != null && p.detectedName().toLowerCase().contains("sakthi"));
    }

    @Test
    void detectsSkillsWithUnknownProficiency() {
        ParsedResumeDTO p = extraction.extract(
                "resume.txt", "text/plain", SAMPLE.length(), "abc", "resumes/x", SAMPLE);

        assertTrue(p.detectedSkills().stream().anyMatch(s -> s.name().equals("Java")));
        assertTrue(p.detectedSkills().stream().anyMatch(s -> s.name().equals("Kafka")));
        // Parser must never assign EXPERT — everything is UNKNOWN until user verifies.
        assertTrue(p.detectedSkills().stream().allMatch(s -> s.proficiency().equals("UNKNOWN")));
    }

    @Test
    void detectsExperienceAndEducation() {
        ParsedResumeDTO p = extraction.extract(
                "resume.txt", "text/plain", SAMPLE.length(), "abc", "resumes/x", SAMPLE);

        assertFalse(p.detectedExperience().isEmpty());
        assertTrue(p.detectedExperience().get(0).current());
        assertFalse(p.detectedEducation().isEmpty());
    }
}

