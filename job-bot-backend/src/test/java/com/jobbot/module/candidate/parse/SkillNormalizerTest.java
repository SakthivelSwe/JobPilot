package com.jobbot.module.candidate.parse;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkillNormalizerTest {

    private final SkillNormalizer normalizer = new SkillNormalizer();

    @Test
    void normalizesCommonAliasesToCanonical() {
        assertEquals("Spring Boot", normalizer.normalize("springboot").canonical());
        assertEquals("Spring Boot", normalizer.normalize("spring-boot").canonical());
        assertEquals("Kafka", normalizer.normalize("apache kafka").canonical());
        assertEquals("Kafka", normalizer.normalize("message broker").canonical());
        assertEquals("AWS", normalizer.normalize("Amazon Web Services").canonical());
        assertEquals("Kubernetes", normalizer.normalize("k8s").canonical());
        assertEquals("Kubernetes", normalizer.normalize("EKS").canonical());
        assertEquals("PostgreSQL", normalizer.normalize("postgres").canonical());
        assertEquals("Java", normalizer.normalize("Core Java").canonical());
    }

    @Test
    void assignsCategories() {
        assertEquals("MESSAGING", normalizer.normalize("kafka").category());
        assertEquals("CLOUD", normalizer.normalize("aws").category());
        assertEquals("CONTAINER", normalizer.normalize("kubernetes").category());
    }

    @Test
    void unknownSkillReturnsNull() {
        assertNull(normalizer.normalize("basket weaving"));
        assertNull(normalizer.normalize(""));
        assertNull(normalizer.normalize(null));
    }

    @Test
    void detectInTextRespectsWordBoundaries() {
        // "java" inside "javascript" must not be detected as Java.
        Map<String, String> found = normalizer.detectInText("Experienced JavaScript developer");
        assertTrue(found.containsKey("JavaScript"));
        assertFalse(found.containsKey("Java"));
    }

    @Test
    void detectInTextFindsMultipleCanonicalSkills() {
        Map<String, String> found = normalizer.detectInText(
                "Built Spring Boot microservices with Kafka on AWS, deployed via Kubernetes.");
        assertTrue(found.keySet().containsAll(
                java.util.List.of("Spring Boot", "Kafka", "AWS", "Kubernetes", "Microservices")));
    }
}

