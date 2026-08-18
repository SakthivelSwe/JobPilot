package com.jobbot.module.ai.usage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI cost protection (spec §55): tracks usage and enforces a configurable daily cap.
 * Callers must check {@link #canCall()} before invoking a paid/metered AI provider and
 * {@link #record} afterwards.
 */
@Service
@RequiredArgsConstructor
public class AiUsageTracker {

    private final AiUsageRepository repository;

    @Value("${app.ai.daily-limit:20}")
    private int dailyLimit;

    /** True when today's total AI requests are below the configured daily cap. */
    public boolean canCall() {
        return repository.totalRequestsOn(LocalDate.now()) < dailyLimit;
    }

    /** Record one AI call. Rough token estimate ≈ chars/4. */
    @Transactional
    public void record(String provider, String feature, long estimatedTokens) {
        LocalDate today = LocalDate.now();
        AiUsage usage = repository.findByUsageDateAndProviderAndFeature(today, provider, feature)
                .orElseGet(() -> AiUsage.builder()
                        .usageDate(today).provider(provider).feature(feature)
                        .requests(0).estimatedTokens(0).build());
        usage.setRequests(usage.getRequests() + 1);
        usage.setEstimatedTokens(usage.getEstimatedTokens() + Math.max(0, estimatedTokens));
        repository.save(usage);
    }

    /** Convenience token estimator from raw text length. */
    public long estimateTokens(String... texts) {
        long chars = 0;
        for (String t : texts) if (t != null) chars += t.length();
        return chars / 4;
    }

    public Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        long used = repository.totalRequestsOn(today);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", today.toString());
        m.put("dailyLimit", dailyLimit);
        m.put("used", used);
        m.put("remaining", Math.max(0, dailyLimit - used));
        m.put("byFeature", repository.findByUsageDate(today).stream()
                .collect(java.util.stream.Collectors.toMap(
                        u -> u.getProvider() + ":" + u.getFeature(),
                        AiUsage::getRequests, Integer::sum, LinkedHashMap::new)));
        return m;
    }
}

