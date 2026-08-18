package com.jobbot.module.matching;

import org.springframework.stereotype.Service;

/**
 * Turns an overall score + hard filters into a {@link Recommendation} (spec §15).
 * Score thresholds mirror the spec examples (95→STRONG, 85→APPLY, 70→REVIEW,
 * 55→LOW, &lt;55→SKIP). Hard filters can only lower the recommendation, never raise it.
 */
@Service
public class RecommendationEngine {

    public Recommendation fromScore(int score) {
        if (score >= 90) return Recommendation.STRONG_APPLY;
        if (score >= 80) return Recommendation.APPLY;
        if (score >= 65) return Recommendation.REVIEW;
        if (score >= 55) return Recommendation.LOW_PRIORITY;
        return Recommendation.SKIP;
    }

    /**
     * @param excludedCompany         job is at a user-excluded company → hard SKIP (§8/§15)
     * @param experienceHardMismatch  candidate experience is far outside the job's range
     * @param missingAllRequired      candidate has none of the required skills
     */
    public Recommendation recommend(int score,
                                    boolean excludedCompany,
                                    boolean experienceHardMismatch,
                                    boolean missingAllRequired) {
        if (excludedCompany) return Recommendation.SKIP;

        Recommendation base = fromScore(score);
        if (experienceHardMismatch) {
            base = lowerTo(base, Recommendation.LOW_PRIORITY);
        }
        if (missingAllRequired) {
            base = lowerTo(base, Recommendation.REVIEW);
        }
        return base;
    }

    /** Returns whichever is the lower (worse) recommendation. */
    private Recommendation lowerTo(Recommendation current, Recommendation cap) {
        // Higher ordinal = worse (STRONG_APPLY=0 ... SKIP=4).
        return current.ordinal() >= cap.ordinal() ? current : cap;
    }
}

