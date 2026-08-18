package com.jobbot.module.manualqueue;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Manual Applications API (spec §27/§42). List, triage, and record manual applications;
 * "mark-applied" flows into the Kanban CRM. No auto-submit anywhere.
 */
@RestController
@RequestMapping("/api/manual-queue")
@RequiredArgsConstructor
public class ManualQueueController {

    private final ManualQueueService service;

    @GetMapping
    public ApiResponse<List<ManualQueueEntry>> list(@RequestParam(required = false) String status) {
        ManualQueueStatus s = null;
        if (status != null && !status.isBlank()) {
            s = ManualQueueStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        }
        return ApiResponse.ok(service.list(s));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.ok(service.stats());
    }

    @PostMapping("/add/{postingId}")
    public ApiResponse<ManualQueueEntry> add(@PathVariable UUID postingId) {
        return ApiResponse.ok(service.add(postingId), "Added to manual queue");
    }

    @PostMapping("/{id}/open")
    public ApiResponse<ManualQueueEntry> open(@PathVariable UUID id) {
        return ApiResponse.ok(service.markOpened(id), "Marked opened");
    }

    @PostMapping("/{id}/mark-applied")
    public ApiResponse<ManualQueueEntry> markApplied(@PathVariable UUID id) {
        return ApiResponse.ok(service.markApplied(id), "Marked applied — added to Kanban");
    }

    @PostMapping("/{id}/skip")
    public ApiResponse<ManualQueueEntry> skip(@PathVariable UUID id) {
        return ApiResponse.ok(service.skip(id), "Skipped");
    }
}

