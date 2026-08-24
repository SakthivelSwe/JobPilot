package com.jobbot.module.platform;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.platform.dto.PlatformConfigUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformConfigService {

    public static final String NAUKRI = "NAUKRI";
    public static final String LINKEDIN = "LINKEDIN";
    public static final String INDEED = "INDEED";

    private final PlatformConfigRepository repository;

    @Transactional
    public List<PlatformConfig> getAll() {
        String userId = com.jobbot.security.SecurityUtils.getCurrentUserId();
        List<PlatformConfig> configs = repository.findAllForCurrentTenant(userId);
        if (configs.isEmpty() && !"anonymousUser".equals(userId) && !"system".equals(userId)) {
            log.info("No platform configs found for user {}, auto-seeding defaults.", userId);
            seedDefaults();
            configs = repository.findAllForCurrentTenant(userId);
        }
        return configs;
    }

    public PlatformConfig get(String platform) {
        return repository.findByPlatformNameIgnoreCase(platform)
                .orElseThrow(() -> new JobBotException("Unknown platform: " + platform));
    }

    /** Checks enabled + dailyLimit + isPaused + resetCountIfNewDay(). */
    @Transactional
    public boolean canApply(String platform) {
        PlatformConfig p = resetIfNewDay(get(platform));
        if (!p.isEnabled()) return false;
        if (p.isPaused()) return false;
        return p.getCurrentCountToday() < p.getDailyLimit();
    }

    @Transactional
    public void incrementCount(String platform) {
        PlatformConfig p = resetIfNewDay(get(platform));
        repository.incrementCount(p.getId());
    }

    @Transactional
    public PlatformConfig resetIfNewDay(PlatformConfig p) {
        LocalDate today = LocalDate.now();
        if (p.getLastResetDate() == null || !p.getLastResetDate().equals(today)) {
            p.setCurrentCountToday(0);
            p.setLastResetDate(today);
            return repository.save(p);
        }
        return p;
    }

    @Transactional
    public PlatformConfig pause(String platform) {
        PlatformConfig p = get(platform);
        p.setPaused(true);
        return repository.save(p);
    }

    @Transactional
    public PlatformConfig resume(String platform) {
        PlatformConfig p = get(platform);
        p.setPaused(false);
        return repository.save(p);
    }

    @Transactional
    public PlatformConfig resetCount(String platform) {
        PlatformConfig p = get(platform);
        p.setCurrentCountToday(0);
        p.setLastResetDate(LocalDate.now());
        return repository.save(p);
    }

    @Transactional
    public PlatformConfig update(String platform, PlatformConfigUpdateDTO dto) {
        PlatformConfig p = get(platform);
        if (dto.getEnabled() != null) p.setEnabled(dto.getEnabled());
        if (dto.getDailyLimit() != null) p.setDailyLimit(dto.getDailyLimit());
        if (dto.getMinDelaySeconds() != null) p.setMinDelaySeconds(dto.getMinDelaySeconds());
        // Credential handling removed (spec §1): no platform account secrets are stored.
        return repository.save(p);
    }


    /** Generic save — used by PlatformSessionService to persist session state changes. */
    @Transactional
    public PlatformConfig saveConfig(PlatformConfig config) {
        return repository.save(config);
    }

    /** Called by DataSeeder / migrations on startup. */
    @Transactional
    public void seedDefaults() {
        seed(NAUKRI, 30, 300);
        seed(LINKEDIN, 15, 350);
        seed(INDEED, 20, 300);
    }

    private void seed(String name, int limit, int delay) {
        if (repository.findByPlatformNameIgnoreCase(name).isPresent()) return;
        repository.save(PlatformConfig.builder()
                .platformName(name)
                .enabled(true)
                .dailyLimit(limit)
                .minDelaySeconds(delay)
                .currentCountToday(0)
                .lastResetDate(LocalDate.now())
                .paused(false)
                .sessionStatus("DISCONNECTED")
                .sessionActive(false)
                .build());
        log.info("Seeded platform config: {}", name);
    }
}

