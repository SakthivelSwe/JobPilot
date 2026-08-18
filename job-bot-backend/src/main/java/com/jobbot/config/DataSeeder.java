package com.jobbot.config;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.criteria.CriteriaRepository;
import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.job.Job;
import com.jobbot.module.job.JobRepository;
import com.jobbot.module.platform.PlatformConfigService;
import com.jobbot.module.resume.Resume;
import com.jobbot.module.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Seeds realistic demo data on first run (local profile only, when DB is empty)
 * so the dashboard and pages look alive immediately.
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ResumeRepository resumeRepo;
    private final CriteriaRepository criteriaRepo;
    private final JobRepository jobRepo;
    private final ApplicationRepository appRepo;
    private final PlatformConfigService platformConfigService;

    @Override
    public void run(String... args) {
        // Always ensure the platform_config table has NAUKRI / LINKEDIN / INDEED rows
        platformConfigService.seedDefaults();

        if (resumeRepo.count() > 0) {
            return;
        }
        log.info("Seeding demo data…");

        Resume backend = resumeRepo.save(Resume.builder()
                .name("Java Backend Senior")
                .targetRoles(List.of("Senior Java Developer", "Backend Engineer"))
                .targetSkills(List.of("Java", "Spring Boot", "Kafka", "AWS", "Microservices", "REST"))
                .resumeText("2.8 years building Java 17 Spring Boot microservices with Kafka, AWS, REST APIs, PostgreSQL. Delivered production integrations at scale.")
                .experienceSummary("a backend engineer with 2.8 years in Java, Spring Boot, Kafka and AWS")
                .active(true).build());

        Resume fullstack = resumeRepo.save(Resume.builder()
                .name("Full Stack Java + Angular")
                .targetRoles(List.of("Full Stack Developer", "Java Angular Developer"))
                .targetSkills(List.of("Java", "Spring Boot", "Angular", "TypeScript", "REST"))
                .resumeText("Full stack engineer: Java Spring Boot backends with Angular 17 frontends, TypeScript, REST APIs, responsive UIs.")
                .experienceSummary("a full stack engineer skilled in Java, Spring Boot and Angular")
                .active(true).build());

        resumeRepo.save(Resume.builder()
                .name("Integration / Middleware")
                .targetRoles(List.of("Integration Engineer", "Middleware Developer"))
                .targetSkills(List.of("Spring Boot", "Kafka", "REST", "Salesforce", "Integration"))
                .resumeText("Middleware and integration specialist: Spring Boot, Kafka event streaming, Salesforce/CXone integrations, REST.")
                .experienceSummary("an integration engineer experienced with Spring Boot, Kafka and Salesforce")
                .active(true).build());

        JobCriteria crit = criteriaRepo.save(JobCriteria.builder()
                .name("Senior Java · Chennai/Remote")
                .resumeId(backend.getId())
                .keywords(List.of("Java", "Spring Boot", "Kafka", "AWS", "Microservices"))
                .locations(List.of("Chennai", "Bangalore", "Remote"))
                .experienceMin(2).experienceMax(5)
                .minMatchScore(BigDecimal.valueOf(65))
                .active(true).build());

        criteriaRepo.save(JobCriteria.builder()
                .name("Full Stack · Remote")
                .resumeId(fullstack.getId())
                .keywords(List.of("Java", "Spring Boot", "Angular", "TypeScript"))
                .locations(List.of("Remote", "Hyderabad"))
                .experienceMin(2).experienceMax(4)
                .minMatchScore(BigDecimal.valueOf(60))
                .active(true).build());

        Job j1 = jobRepo.save(Job.builder()
                .platform("linkedin").platformJobId("seed-1")
                .title("Senior Java Backend Engineer").company("FinEdge")
                .location("Chennai").status("matched")
                .url("https://example.com/jobs/seed-1")
                .description("Java Spring Boot Kafka microservices REST AWS PostgreSQL in Chennai. Design scalable services.")
                .matchScore(BigDecimal.valueOf(92))
                .matchKeywords(List.of("Java", "Spring Boot", "Kafka", "AWS", "REST"))
                .missingKeywords(List.of("Microservices"))
                .reasonToApply("92% MATCH\n\nStrong overlap on Java, Spring Boot, Kafka, AWS.\nDecision: STRONG APPLY")
                .criteriaId(crit.getId()).build());

        Job j2 = jobRepo.save(Job.builder()
                .platform("naukri").platformJobId("seed-2")
                .title("Java Developer - Microservices").company("CloudNine")
                .location("Bangalore").status("applied")
                .url("https://example.com/jobs/seed-2")
                .description("Spring Boot microservices, Kafka, REST APIs. AWS a plus.")
                .matchScore(BigDecimal.valueOf(78))
                .matchKeywords(List.of("Spring Boot", "Kafka", "REST"))
                .missingKeywords(List.of("AWS", "Java"))
                .criteriaId(crit.getId()).build());

        jobRepo.save(Job.builder()
                .platform("indeed").platformJobId("seed-3")
                .title("Full Stack Engineer (Angular + Java)").company("BrightApps")
                .location("Remote").status("matched")
                .url("https://example.com/jobs/seed-3")
                .description("Angular 17 TypeScript frontend with Java Spring Boot backend, REST APIs.")
                .matchScore(BigDecimal.valueOf(71))
                .matchKeywords(List.of("Angular", "TypeScript", "Java", "Spring Boot"))
                .missingKeywords(List.of())
                .criteriaId(crit.getId()).build());

        appRepo.save(Application.builder()
                .jobId(j2.getId()).resumeId(backend.getId()).criteriaId(crit.getId())
                .platform("naukri").company("CloudNine").title("Java Developer - Microservices")
                .status("interview").atsScore(BigDecimal.valueOf(78))
                .interviewRound(1).interviewDate(OffsetDateTime.now().plusDays(3))
                .matchedKeywords(List.of("Spring Boot", "Kafka", "REST"))
                .build());

        appRepo.save(Application.builder()
                .jobId(j1.getId()).resumeId(backend.getId()).criteriaId(crit.getId())
                .platform("linkedin").company("FinEdge").title("Senior Java Backend Engineer")
                .status("applied").atsScore(BigDecimal.valueOf(92))
                .matchedKeywords(List.of("Java", "Spring Boot", "Kafka"))
                .build());

        log.info("Demo data seeded: {} resumes, {} criteria, {} jobs, {} applications",
                resumeRepo.count(), criteriaRepo.count(), jobRepo.count(), appRepo.count());
    }
}

