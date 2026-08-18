package com.jobbot.module.ai;

/**
 * Pluggable AI layer. The core app works with NoOpAiProvider (default),
 * so no paid/external AI service is ever required.
 */
public interface AiProvider {

    /** @return true if this provider is configured and reachable. */
    boolean isAvailable();

    /**
     * Optional AI enrichment on top of the deterministic ATS result.
     * Returns a short human-readable note, or null if unavailable.
     */
    String enrich(String resumeText, String jobDescription);
}

