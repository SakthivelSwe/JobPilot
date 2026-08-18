package com.jobbot.module.application;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.application.dto.ApplicationCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;

    @GetMapping
    public ApiResponse<Page<Application>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return ApiResponse.ok(service.findAll(pr));
    }

    @GetMapping("/kanban")
    public ApiResponse<Map<String, List<Application>>> kanban() {
        return ApiResponse.ok(service.getKanban());
    }

    @GetMapping("/{id}")
    public ApiResponse<Application> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    public ApiResponse<Application> create(@RequestBody ApplicationCreateDTO dto) {
        return ApiResponse.ok(service.create(dto), "Application recorded");
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Application> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(service.updateStatus(id, body.get("status"), body.get("notes")), "Status updated");
    }

    @PostMapping("/{id}/notes")
    public ApiResponse<Application> addNote(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(service.updateStatus(id,
                service.findById(id).getStatus(), body.get("notes")), "Note added");
    }

    @PutMapping("/{id}/interview")
    public ApiResponse<Application> setInterview(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        OffsetDateTime date = body.get("interviewDate") != null
                ? OffsetDateTime.parse(body.get("interviewDate").toString()) : null;
        Integer round = body.get("interviewRound") != null
                ? Integer.valueOf(body.get("interviewRound").toString()) : null;
        return ApiResponse.ok(service.setInterview(id, date, round), "Interview set");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Application deleted");
    }
}

