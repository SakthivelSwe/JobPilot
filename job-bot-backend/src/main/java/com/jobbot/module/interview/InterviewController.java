package com.jobbot.module.interview;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService service;

    @GetMapping
    public ApiResponse<List<InterviewService.InterviewRef>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{applicationId}/prep")
    public ApiResponse<InterviewService.PrepPack> prep(@PathVariable UUID applicationId) {
        return ApiResponse.ok(service.prep(applicationId));
    }
}

