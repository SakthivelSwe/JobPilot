package com.jobbot.module.queue;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class JobQueueController {

    private final JobQueueService service;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<JobQueueEntry>>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPendingReview(PageRequest.of(page, size))));
    }

    @GetMapping("/auto-applying")
    public ResponseEntity<ApiResponse<Page<JobQueueEntry>>> autoApplying(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAutoApplying(PageRequest.of(page, size))));
    }

    @GetMapping("/manual")
    public ResponseEntity<ApiResponse<Page<JobQueueEntry>>> manual(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getManualQueue(PageRequest.of(page, size))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(service.stats()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<JobQueueEntry>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.approve(id)));
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<ApiResponse<JobQueueEntry>> skip(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.skip(id)));
    }

    @PostMapping("/{id}/send-to-manual")
    public ResponseEntity<ApiResponse<JobQueueEntry>> sendToManual(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.sendToManual(id)));
    }

    @PostMapping("/{id}/mark-applied")
    public ResponseEntity<ApiResponse<JobQueueEntry>> markApplied(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.markApplied(id)));
    }

    @PostMapping("/approve-all-above")
    public ResponseEntity<ApiResponse<Integer>> approveAllAbove(
            @RequestParam(defaultValue = "80") int threshold) {
        int n = service.approveAllAbove(BigDecimal.valueOf(threshold));
        return ResponseEntity.ok(ApiResponse.ok(n));
    }

    @PostMapping("/approve-bulk")
    public ResponseEntity<ApiResponse<Integer>> approveBulk(@RequestBody java.util.List<UUID> postingIds) {
        int n = service.bulkApproveByPostingIds(postingIds);
        return ResponseEntity.ok(ApiResponse.ok(n));
    }
}

