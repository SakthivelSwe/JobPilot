package com.jobbot.module.ats.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AtsResult {

    private int score;
    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();
    @Builder.Default
    private List<String> missingKeywords = new ArrayList<>();
    private String bestResumeAngle;
    private String suggestions;
    private boolean shouldApply;
    private String reasonToApply;
    /** Weighted sub-scores (technical, experience, location, ...). */
    @Builder.Default
    private Map<String, Integer> breakdown = new LinkedHashMap<>();
    /** Optional AI enrichment note; null when AI is disabled. */
    private String aiNote;

    public static AtsResult failed(String reason) {
        return AtsResult.builder()
                .score(0)
                .shouldApply(false)
                .suggestions(reason)
                .reasonToApply("Analysis failed: " + reason)
                .build();
    }
}

