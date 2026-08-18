package com.jobbot.module.resume;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.resume.dto.ResumeCreateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService service;

    @GetMapping
    public ApiResponse<List<Resume>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Resume> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    public ApiResponse<Resume> create(@Valid @RequestBody ResumeCreateDTO dto) {
        return ApiResponse.ok(service.create(dto), "Resume created");
    }

    @PutMapping("/{id}")
    public ApiResponse<Resume> update(@PathVariable UUID id, @Valid @RequestBody ResumeCreateDTO dto) {
        return ApiResponse.ok(service.update(id, dto), "Resume updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Resume deleted");
    }
}

