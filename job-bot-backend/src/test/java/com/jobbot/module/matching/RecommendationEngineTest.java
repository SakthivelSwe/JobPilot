package com.jobbot.module.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void scoreThresholds() {
        assertEquals(Recommendation.STRONG_APPLY, engine.fromScore(95));
        assertEquals(Recommendation.APPLY, engine.fromScore(85));
        assertEquals(Recommendation.REVIEW, engine.fromScore(70));
        assertEquals(Recommendation.LOW_PRIORITY, engine.fromScore(55));
        assertEquals(Recommendation.SKIP, engine.fromScore(40));
    }

    @Test
    void excludedCompanyForcesSkip() {
        assertEquals(Recommendation.SKIP, engine.recommend(98, true, false, false));
    }

    @Test
    void experienceMismatchCapsAtLowPriority() {
        // Would be STRONG_APPLY on score, capped to LOW_PRIORITY.
        assertEquals(Recommendation.LOW_PRIORITY, engine.recommend(95, false, true, false));
    }

    @Test
    void missingAllRequiredCapsAtReview() {
        assertEquals(Recommendation.REVIEW, engine.recommend(92, false, false, true));
    }

    @Test
    void capsNeverRaiseRecommendation() {
        // Already SKIP on score; a REVIEW cap must not improve it.
        assertEquals(Recommendation.SKIP, engine.recommend(30, false, false, true));
    }
}

