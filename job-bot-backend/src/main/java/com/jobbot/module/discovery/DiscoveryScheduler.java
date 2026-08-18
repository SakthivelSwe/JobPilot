package com.jobbot.module.discovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optional scheduled discovery (spec §56). DISABLED by default — enable with
 * {@code app.discovery.scan.enabled=true}. It only scans AUTHORIZED PUBLIC FEEDS
 * (Greenhouse / Ashby public APIs) via {@link JobDiscoveryService}; it never touches
 * restricted platforms and never submits applications (spec §1/§26).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscoveryScheduler {

    private final JobDiscoveryService discoveryService;

    @Value("${app.discovery.scan.enabled:false}")
    private boolean enabled;

    /** Default: every 6 hours. Configure via {@code app.discovery.scan.cron}. */
    @Scheduled(cron = "${app.discovery.scan.cron:0 0 */6 * * *}")
    public void scheduledScan() {
        if (!enabled) {
            return; // opt-in only
        }
        try {
            DiscoveryResult r = discoveryService.scan();
            log.info("Scheduled discovery: {} new postings, {} duplicates, {} errors across {} sources",
                    r.newPostings(), r.crossSourceDuplicates(), r.errors(), r.companiesScanned());
        } catch (Exception e) {
            log.warn("Scheduled discovery failed: {}", e.getMessage());
        }
    }
}

