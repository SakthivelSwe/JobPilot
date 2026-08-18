package com.jobbot.module.analytics;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Analytics + learning API (spec §30–31). Read-only aggregation for the dashboard.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;
    private final MomentumService momentumService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(service.overview());
    }

    @GetMapping("/momentum")
    public ApiResponse<MomentumService.Momentum> momentum() {
        return ApiResponse.ok(momentumService.compute());
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.ok(service.rolePerformance());
    }

    @GetMapping("/sources")
    public ApiResponse<List<Map<String, Object>>> sources() {
        return ApiResponse.ok(service.sourcePerformance());
    }

    @GetMapping("/locations")
    public ApiResponse<List<Map<String, Object>>> locations() {
        return ApiResponse.ok(service.locationPerformance());
    }

    @GetMapping("/learning")
    public ApiResponse<Map<String, Object>> learning() {
        return ApiResponse.ok(service.learning());
    }
}

