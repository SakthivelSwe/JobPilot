package com.jobbot.config;

import com.jobbot.module.platform.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Always seeds platform_config (all profiles). Runs before DataSeeder. */
@Component
@Profile("!local") // local is covered by DataSeeder
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PlatformSeeder implements CommandLineRunner {

    private final PlatformConfigService platformConfigService;

    @Override
    public void run(String... args) {
        platformConfigService.seedDefaults();
        log.info("Platform configs verified");
    }
}

