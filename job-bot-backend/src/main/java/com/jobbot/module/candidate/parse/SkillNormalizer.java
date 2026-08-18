package com.jobbot.module.candidate.parse;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes messy skill mentions to canonical names (spec §5). Alias-driven and
 * deterministic — no AI. Also assigns a coarse category. Never assigns proficiency.
 */
@Component
public class SkillNormalizer {

    // canonical -> aliases (all lowercase). First key is the canonical display name.
    private static final Map<String, Set<String>> CANON = new LinkedHashMap<>();
    // canonical -> category
    private static final Map<String, String> CATEGORY = new LinkedHashMap<>();

    private static void reg(String canonical, String category, String... aliases) {
        Set<String> set = new LinkedHashSet<>();
        set.add(canonical.toLowerCase(Locale.ROOT));
        for (String a : aliases) set.add(a.toLowerCase(Locale.ROOT));
        CANON.put(canonical, set);
        CATEGORY.put(canonical, category);
    }

    static {
        reg("Java", "LANGUAGE", "core java", "java 8", "java 11", "java 17", "java 21", "j2ee");
        reg("Kotlin", "LANGUAGE");
        reg("TypeScript", "LANGUAGE", "ts");
        reg("JavaScript", "LANGUAGE", "js", "es6");
        reg("Python", "LANGUAGE");
        reg("SQL", "LANGUAGE", "pl/sql", "t-sql");

        reg("Spring Boot", "FRAMEWORK", "springboot", "spring-boot");
        reg("Spring Framework", "FRAMEWORK", "spring", "spring mvc", "spring core");
        reg("Spring Security", "FRAMEWORK");
        reg("Spring Data JPA", "FRAMEWORK", "spring data", "jpa", "hibernate");
        reg("Angular", "FRAMEWORK", "angular 2+", "angular17", "angular 17", "angular18");
        reg("React", "FRAMEWORK", "reactjs", "react.js");
        reg("Node.js", "FRAMEWORK", "node", "nodejs");

        reg("Kafka", "MESSAGING", "apache kafka", "message broker", "event streaming");
        reg("RabbitMQ", "MESSAGING");

        reg("AWS", "CLOUD", "amazon web services", "aws cloud");
        reg("Azure", "CLOUD", "microsoft azure");
        reg("GCP", "CLOUD", "google cloud", "google cloud platform");

        reg("Docker", "CONTAINER", "containers");
        reg("Kubernetes", "CONTAINER", "k8s", "eks", "aks", "gke", "container orchestration");

        reg("PostgreSQL", "DATABASE", "postgres", "postgre sql");
        reg("MySQL", "DATABASE");
        reg("MongoDB", "DATABASE", "mongo");
        reg("Redis", "DATABASE");
        reg("Oracle", "DATABASE", "oracle db");

        reg("REST", "API", "rest api", "restful", "rest apis");
        reg("GraphQL", "API");
        reg("Microservices", "ARCHITECTURE", "micro-services", "microservice");

        reg("Git", "TOOL", "github", "gitlab", "bitbucket");
        reg("Jenkins", "TOOL", "ci/cd", "cicd");
        reg("Maven", "TOOL");
        reg("Gradle", "TOOL");
        reg("Kibana", "TOOL");
        reg("Salesforce", "PLATFORM", "sfdc");
    }

    public record Normalized(String canonical, String category) {}

    /** Returns the canonical form + category for a raw token, or null if not recognized. */
    public Normalized normalize(String raw) {
        if (raw == null) return null;
        String t = raw.toLowerCase(Locale.ROOT).trim();
        if (t.isEmpty()) return null;
        for (var e : CANON.entrySet()) {
            if (e.getValue().contains(t)) {
                return new Normalized(e.getKey(), CATEGORY.get(e.getKey()));
            }
        }
        return null;
    }

    /** Scans free text and returns the set of canonical skills mentioned. */
    public Map<String, String> detectInText(String text) {
        Map<String, String> found = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return found;
        String lower = text.toLowerCase(Locale.ROOT);
        for (var e : CANON.entrySet()) {
            for (String alias : e.getValue()) {
                if (containsWord(lower, alias)) {
                    found.put(e.getKey(), CATEGORY.get(e.getKey()));
                    break;
                }
            }
        }
        return found;
    }

    private static boolean containsWord(String haystack, String needle) {
        int idx = haystack.indexOf(needle);
        while (idx >= 0) {
            boolean leftOk = idx == 0 || !Character.isLetterOrDigit(haystack.charAt(idx - 1));
            int end = idx + needle.length();
            boolean rightOk = end >= haystack.length() || !Character.isLetterOrDigit(haystack.charAt(end));
            if (leftOk && rightOk) return true;
            idx = haystack.indexOf(needle, idx + 1);
        }
        return false;
    }
}

