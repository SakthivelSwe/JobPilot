package com.jobbot.module.queue;

import com.jobbot.common.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * The application-engine (local Node.js) and the Chrome Extension poll these
 * endpoints. Standard JWT auth applies — the caller logs in as the user, no
 * separate machine role.
 */
@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
@Slf4j
public class EngineController {

    private final JobQueueService service;

    /**
     * Fetch the next APPROVED job for a platform. Atomically transitions it to
     * AUTO_APPLYING so no other worker (or a duplicate engine instance) picks
     * the same entry. Returns 204 when nothing is pending.
     */
    @GetMapping("/pending")
    public ResponseEntity<JobQueueEntry> pending(@RequestParam String platform) {
        Optional<JobQueueEntry> next = service.pickNextApproved(platform);
        return next.<ResponseEntity<JobQueueEntry>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Engine reports success/failure for an in-flight apply.
     */
    @PostMapping("/report")
    public ResponseEntity<ApiResponse<JobQueueEntry>> report(@RequestBody ReportRequest body) {
        JobQueueEntry updated = body.success
                ? service.markAutoApplied(body.jobQueueId)
                : service.markFailed(body.jobQueueId, body.failureReason);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @Data
    public static class ReportRequest {
        @NotNull
        private UUID jobQueueId;
        private boolean success;
        private String failureReason;
    }
}

