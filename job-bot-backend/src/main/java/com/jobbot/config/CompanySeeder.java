package com.jobbot.config;

import com.jobbot.module.company.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Company registry seeder. India-first: no ATS companies are seeded by default —
 * discovery flows via search-based adapters (Naukri / LinkedIn / Indeed) keyed off
 * the user's own {@code TargetRole} + {@code JobCriteria}. Add ATS-backed companies
 * (Greenhouse/Ashby) manually via the Companies page when useful.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class CompanySeeder implements CommandLineRunner {

    private final CompanyService companyService;

    @Override
    public void run(String... args) {
        // Intentionally no seed data. Companies are user-managed.
        log.info("CompanySeeder: no default companies (India-first, search-based discovery).");
    }
}
