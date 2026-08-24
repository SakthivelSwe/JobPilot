package com.jobbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration: upgrades the platform_config unique constraint from
 * (platform_name) to (platform_name, user_id) to support multi-tenant configs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformConfigMigration {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        // Step 1: Drop ALL unique constraints on platform_config.platform_name that may have been
        // created by old entity definitions or Hibernate DDL-auto (they have generated hash names)
        dropAllPlatformNameConstraints();

        // Step 2: Ensure our correct composite constraint exists
        try {
            jdbc.execute(
                "ALTER TABLE platform_config " +
                "ADD CONSTRAINT platform_config_platform_name_user_id_key UNIQUE (platform_name, user_id)"
            );
            log.info("PlatformConfigMigration: created composite unique constraint (platform_name, user_id)");
        } catch (Exception e) {
            log.debug("PlatformConfigMigration: composite constraint already exists, skipped: {}", e.getMessage());
        }
    }

    private void dropAllPlatformNameConstraints() {
        try {
            // Find and drop any unique constraint on just platform_name (single-column)
            var constraints = jdbc.queryForList(
                "SELECT conname FROM pg_constraint c " +
                "JOIN pg_class t ON t.oid = c.conrelid " +
                "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey) " +
                "WHERE t.relname = 'platform_config' " +
                "AND c.contype = 'u' " +
                "AND a.attname = 'platform_name' " +
                "GROUP BY conname " +
                "HAVING count(*) = 1"  // only single-column constraints on platform_name
            );
            for (var row : constraints) {
                String name = (String) row.get("conname");
                try {
                    jdbc.execute("ALTER TABLE platform_config DROP CONSTRAINT IF EXISTS \"" + name + "\"");
                    log.info("PlatformConfigMigration: dropped old constraint '{}'", name);
                } catch (Exception ex) {
                    log.warn("PlatformConfigMigration: could not drop constraint '{}': {}", name, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("PlatformConfigMigration: error scanning constraints: {}", e.getMessage());
        }
    }
}
