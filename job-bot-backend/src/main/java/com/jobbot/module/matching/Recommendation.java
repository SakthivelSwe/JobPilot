package com.jobbot.module.matching;

/**
 * Recommendation buckets (spec §15). Score alone is not enough — hard filters can
 * cap the recommendation regardless of score.
 */
public enum Recommendation {
    STRONG_APPLY,
    APPLY,
    REVIEW,
    LOW_PRIORITY,
    SKIP
}

