package com.jobbot.module.role;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.role.dto.TargetRoleDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Target Role Engine API (spec §7).
 */
@RestController
@RequestMapping("/api/target-roles")
@RequiredArgsConstructor
public class TargetRoleController {

    private final TargetRoleService service;

    @GetMapping
    public ApiResponse<List<TargetRole>> list(@RequestParam(required = false) Boolean activeOnly) {
        return ApiResponse.ok(Boolean.TRUE.equals(activeOnly) ? service.getActive() : service.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<TargetRole> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    public ApiResponse<TargetRole> create(@Valid @RequestBody TargetRoleDTO dto) {
        return ApiResponse.ok(service.create(dto), "Target role created");
    }

    @PutMapping("/{id}")
    public ApiResponse<TargetRole> update(@PathVariable UUID id, @RequestBody TargetRoleDTO dto) {
        return ApiResponse.ok(service.update(id, dto), "Target role updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Target role deleted");
    }

    /** Body: { "<roleId>": <priority>, ... } */
    @PostMapping("/reorder")
    public ApiResponse<List<TargetRole>> reorder(@RequestBody Map<UUID, Integer> priorities) {
        return ApiResponse.ok(service.reorder(priorities), "Priorities updated");
    }
}

