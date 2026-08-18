package com.jobbot.module.job;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.job.dto.JobImportDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService service;

    @GetMapping
    public ApiResponse<Page<Job>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scrapedAt"));
        return ApiResponse.ok(service.findAll(status, platform, pr));
    }

    @GetMapping("/{id}")
    public ApiResponse<Job> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/import")
    public ApiResponse<Job> importJob(@Valid @RequestBody JobImportDTO dto) {
        return ApiResponse.ok(service.importJob(dto), "Job imported");
    }

    @PostMapping("/{id}/score")
    public ApiResponse<Job> score(@PathVariable UUID id, @RequestBody Map<String, UUID> body) {
        UUID criteriaId = body.get("criteriaId");
        UUID resumeId = body.get("resumeId");
        if (criteriaId != null) {
            return ApiResponse.ok(service.scoreAgainstCriteria(id, criteriaId), "Scored");
        }
        return ApiResponse.ok(service.scoreAgainstResume(id, resumeId), "Scored");
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Job> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(service.updateStatus(id, body.get("status")), "Status updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Job deleted");
    }
}

