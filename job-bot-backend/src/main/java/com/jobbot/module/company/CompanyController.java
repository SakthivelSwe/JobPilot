package com.jobbot.module.company;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.company.dto.CompanyDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Company registry API (spec §10).
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;
    private final CompanyInsightService insightService;

    @GetMapping
    public ApiResponse<List<Company>> list(@RequestParam(required = false) Boolean activeOnly) {
        return ApiResponse.ok(Boolean.TRUE.equals(activeOnly) ? service.getActive() : service.getAll());
    }

    /** Company-centric aggregation of the user's postings/applications/saved items. */
    @GetMapping("/overview")
    public ApiResponse<CompanyInsightService.CompanyOverview> overview(@RequestParam String name) {
        return ApiResponse.ok(insightService.overview(name));
    }

    @GetMapping("/{id}")
    public ApiResponse<Company> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    public ApiResponse<Company> create(@Valid @RequestBody CompanyDTO dto) {
        return ApiResponse.ok(service.create(dto), "Company added");
    }

    @PutMapping("/{id}")
    public ApiResponse<Company> update(@PathVariable UUID id, @RequestBody CompanyDTO dto) {
        return ApiResponse.ok(service.update(id, dto), "Company updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Company deleted");
    }
}

