package com.jobbot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory brute-force protection for login (spec §49). After too many
 * failed attempts a key (username) is locked out for a cool-down window. Suitable for
 * the single-user deployment; a distributed setup would use a shared store.
 */
@Component
public class LoginRateLimiter {

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lock-minutes:15}")
    private long lockMinutes;

    private static final class Attempt {
        int failures;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        Attempt a = attempts.get(key);
        if (a == null || a.lockedUntil == null) return false;
        if (Instant.now().isAfter(a.lockedUntil)) {
            attempts.remove(key);
            return false;
        }
        return true;
    }

    public void recordFailure(String key) {
        Attempt a = attempts.computeIfAbsent(key, k -> new Attempt());
        a.failures++;
        if (a.failures >= maxAttempts) {
            a.lockedUntil = Instant.now().plus(Duration.ofMinutes(lockMinutes));
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}

