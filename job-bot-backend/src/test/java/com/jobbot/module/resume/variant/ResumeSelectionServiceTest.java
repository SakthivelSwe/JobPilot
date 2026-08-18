package com.jobbot.module.resume.variant;

import com.jobbot.module.discovery.JobPosting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeSelectionServiceTest {

    private final ResumeSelectionService service = new ResumeSelectionService();

    @Test
    void microservicesJobSelectsMicroservicesVariant() {
        JobPosting job = JobPosting.builder()
                .title("Java Microservices Engineer")
                .requiredSkills(List.of("Java", "Kafka", "Microservices"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .description("Build event-driven microservices with Kafka, Docker, Kubernetes.")
                .build();
        VariantScore best = service.selectBest(job);
        assertEquals(ResumeVariant.JAVA_MICROSERVICES, best.variant());
        assertTrue(best.recommended());
        assertTrue(best.matchedPrioritySkills().contains("Kafka"));
    }

    @Test
    void fullStackJobSelectsFullStackVariant() {
        JobPosting job = JobPosting.builder()
                .title("Full Stack Engineer")
                .requiredSkills(List.of("Java", "Spring Boot", "Angular", "TypeScript"))
                .description("Angular + TypeScript frontend with Java Spring Boot backend.")
                .build();
        assertEquals(ResumeVariant.JAVA_FULLSTACK, service.selectBest(job).variant());
    }

    @Test
    void rankReturnsAllFourVariantsSortedDescending() {
        JobPosting job = JobPosting.builder().title("Java Developer")
                .requiredSkills(List.of("Java", "AWS")).description("AWS cloud Java").build();
        List<VariantScore> ranked = service.rank(job);
        assertEquals(4, ranked.size());
        for (int i = 1; i < ranked.size(); i++) {
            assertTrue(ranked.get(i - 1).score() >= ranked.get(i).score());
        }
    }
}

