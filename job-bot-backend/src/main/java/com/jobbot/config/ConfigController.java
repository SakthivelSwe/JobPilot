package com.jobbot.config;

import com.jobbot.common.ApiResponse;
import com.jobbot.common.JobPilotThresholds;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the centralized business thresholds so the frontend never hard-codes its
 * own copies (spec rule 68). One source of truth: {@link JobPilotThresholds}.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @GetMapping("/thresholds")
    public ApiResponse<Map<String, Integer>> thresholds() {
        return ApiResponse.ok(Map.of(
                "learningMinApplications", JobPilotThresholds.LEARNING_MIN_APPLICATIONS,
                "defaultMinMatchScore", JobPilotThresholds.DEFAULT_MIN_MATCH_SCORE,
                "strongMatchScore", JobPilotThresholds.STRONG_MATCH_SCORE,
                "followUpDays", JobPilotThresholds.FOLLOW_UP_DAYS,
                "recommendStrongApply", JobPilotThresholds.RECOMMEND_STRONG_APPLY,
                "recommendApply", JobPilotThresholds.RECOMMEND_APPLY,
                "recommendReview", JobPilotThresholds.RECOMMEND_REVIEW,
                "recommendLowPriority", JobPilotThresholds.RECOMMEND_LOW_PRIORITY
        ));
    }
}

