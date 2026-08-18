package com.jobbot.module.activity;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService service;

    @GetMapping
    public ApiResponse<List<ActivityEvent>> recent(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.recent(limit));
    }
}

