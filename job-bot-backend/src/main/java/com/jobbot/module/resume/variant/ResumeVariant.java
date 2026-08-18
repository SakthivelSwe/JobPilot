package com.jobbot.module.resume.variant;

import java.util.List;

/**
 * The four canonical resume variants (spec §19). Each derives from the SAME master
 * candidate profile — never a separate source of truth. The variant only changes
 * emphasis (priority skills, role target, summary angle).
 */
public enum ResumeVariant {

    JAVA_BACKEND("Java Backend Developer",
            List.of("Java", "Spring Boot", "REST", "Microservices", "SQL", "PostgreSQL", "Hibernate")),

    JAVA_FULLSTACK("Java Full Stack Developer",
            List.of("Java", "Spring Boot", "Angular", "TypeScript", "REST", "HTML", "CSS")),

    JAVA_MICROSERVICES("Java Microservices Developer",
            List.of("Java", "Spring Boot", "Kafka", "Microservices", "Docker", "Kubernetes", "REST")),

    JAVA_CLOUD("Java AWS Cloud Developer",
            List.of("Java", "Spring Boot", "AWS", "Docker", "Kubernetes", "Terraform", "Microservices"));

    private final String roleTarget;
    private final List<String> prioritySkills;

    ResumeVariant(String roleTarget, List<String> prioritySkills) {
        this.roleTarget = roleTarget;
        this.prioritySkills = prioritySkills;
    }

    public String roleTarget() {
        return roleTarget;
    }

    public List<String> prioritySkills() {
        return prioritySkills;
    }
}

