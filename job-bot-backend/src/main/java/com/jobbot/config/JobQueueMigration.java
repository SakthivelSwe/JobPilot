package com.jobbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration: adds user_id column to job_queue table for multi-tenant isolation.
 * Existing rows get a placeholder 'system' user_id so the NOT NULL constraint can be applied.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class JobQueueMigration {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        // Step 1: Add user_id column if it doesn't exist (nullable first)
        try {
            jdbc.execute(
                "ALTER TABLE job_queue ADD COLUMN IF NOT EXISTS user_id VARCHAR(255)"
            );
            log.info("JobQueueMigration: ensured user_id column exists on job_queue");
        } catch (Exception e) {
            log.debug("JobQueueMigration: user_id column already exists: {}", e.getMessage());
        }

        // Step 2: Fill any NULL user_id rows with a placeholder (so NOT NULL constraint can be added)
        try {
            int updated = jdbc.update(
                "UPDATE job_queue SET user_id = 'system' WHERE user_id IS NULL"
            );
            if (updated > 0) {
                log.info("JobQueueMigration: back-filled {} job_queue rows with placeholder user_id='system'", updated);
            }
        } catch (Exception e) {
            log.warn("JobQueueMigration: failed to back-fill user_id: {}", e.getMessage());
        }

        // Step 3: Add unique constraint per (external_id, platform, user_id) if not exists
        try {
            jdbc.execute(
                "ALTER TABLE job_queue ADD CONSTRAINT job_queue_external_platform_user_key " +
                "UNIQUE (external_id, platform, user_id)"
            );
            log.info("JobQueueMigration: created composite unique constraint (external_id, platform, user_id)");
        } catch (Exception e) {
            log.debug("JobQueueMigration: composite constraint already exists or failed: {}", e.getMessage());
        }
    }
}
