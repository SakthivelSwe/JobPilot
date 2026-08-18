package com.jobbot.module.ai.usage;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** AI usage / cost-protection stats (spec §55). */
@RestController
@RequestMapping("/api/ai/usage")
@RequiredArgsConstructor
public class AiUsageController {

    private final AiUsageTracker tracker;

    @GetMapping
    public ApiResponse<Map<String, Object>> usage() {
        return ApiResponse.ok(tracker.stats());
    }
}

