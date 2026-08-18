package com.jobbot.module.discovery;

/**
 * ATS / source type. India-focused set:
 *  - {@link #NAUKRI} and {@link #INDEED}: search-based scrapers, ASSISTED_APPLY
 *    (rate-limited; user opt-in per platform).
 *  - {@link #LINKEDIN}: search-based discovery only; application handled
 *    exclusively by the Chrome Extension. Always MANUAL_REQUIRED for the
 *    server-side application-engine (§26).
 *  - {@link #MANUAL}: manual URL import.
 *  - {@link #GREENHOUSE}, {@link #ASHBY}, {@link #LEVER}, {@link #WORKABLE}:
 *    reserved for future ATS integrations (no active adapter shipped).
 */
public enum AtsType {
    NAUKRI,
    LINKEDIN,
    INDEED,
    GREENHOUSE,
    ASHBY,
    LEVER,
    WORKABLE,
    MANUAL,
    OTHER
}
