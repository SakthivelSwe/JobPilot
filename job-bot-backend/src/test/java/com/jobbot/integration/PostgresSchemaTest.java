package com.jobbot.integration;

import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the entity schema is genuinely PostgreSQL-compatible (spec §50/§71) — not
 * just H2. Boots the FULL Spring context against a real Postgres container and exercises
 * a persistence round-trip, including the portable {@code StringListConverter} columns.
 *
 * <p>{@code disabledWithoutDocker = true} means this is **skipped cleanly** when Docker
 * isn't available, so it never breaks a Docker-less build (§50 "where practical").
 */
@SpringBootTest
@ActiveProfiles("pgtest")
@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Let Hibernate create the schema on Postgres to prove entity compatibility.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    JobPostingRepository postingRepository;

    @Test
    void schemaCreatesAndRoundTripsOnRealPostgres() {
        JobPosting posting = JobPosting.builder()
                .source(AtsType.GREENHOUSE)
                .externalId("it-1")
                .title("Java Backend Engineer")
                .company("Acme")
                .location("Bengaluru")
                .requiredSkills(List.of("Java", "Spring Boot"))   // StringListConverter → TEXT
                .build();

        postingRepository.save(posting);

        var found = postingRepository.findBySourceAndExternalId(AtsType.GREENHOUSE, "it-1");
        assertTrue(found.isPresent());
        assertEquals("Java Backend Engineer", found.get().getTitle());
        assertEquals(List.of("Java", "Spring Boot"), found.get().getRequiredSkills());
    }
}
