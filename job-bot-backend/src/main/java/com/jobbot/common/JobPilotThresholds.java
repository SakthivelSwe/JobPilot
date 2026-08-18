package com.jobbot.common;

import java.math.BigDecimal;

/**
 * Single source of truth for JobPilot business thresholds (spec rule 68).
 * Do NOT hard-code these numbers anywhere else — reference these constants so the
 * backend, analytics and (via {@code /api/config/thresholds}) the frontend all agree.
 */
public final class JobPilotThresholds {

    private JobPilotThresholds() {}

    /** Minimum applications before the learning engine may emit recommendations (spec §16). */
    public static final int LEARNING_MIN_APPLICATIONS = 20;

    /** Default criteria match threshold when the user hasn't set one. */
    public static final int DEFAULT_MIN_MATCH_SCORE = 65;
    public static final BigDecimal DEFAULT_MIN_MATCH_SCORE_BD = BigDecimal.valueOf(DEFAULT_MIN_MATCH_SCORE);

    /** A posting at/above this match score is surfaced as a "strong" opportunity (UI + analytics). */
    public static final int STRONG_MATCH_SCORE = 80;

    /** An application older than this (days) with no movement is flagged for follow-up. */
    public static final int FOLLOW_UP_DAYS = 5;

    // Recommendation cutoffs (mirror RecommendationEngine).
    public static final int RECOMMEND_STRONG_APPLY = 90;
    public static final int RECOMMEND_APPLY = 80;
    public static final int RECOMMEND_REVIEW = 70;
    public static final int RECOMMEND_LOW_PRIORITY = 55;
}

