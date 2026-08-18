package com.jobbot.module.ats.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** One resume's fit against a given job — used for auto resume-selection. */
@Data
@Builder
public class ResumeMatch {
    private UUID resumeId;
    private String resumeName;
    private int score;
    private boolean shouldApply;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private String bestResumeAngle;
    private boolean recommended;
}

