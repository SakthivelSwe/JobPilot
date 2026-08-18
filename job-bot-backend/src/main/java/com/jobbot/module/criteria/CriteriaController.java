package com.jobbot.module.criteria;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.criteria.dto.CriteriaCreateDTO;
import com.jobbot.module.criteria.expression.BooleanQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/criteria")
@RequiredArgsConstructor
public class CriteriaController {

    private final CriteriaService service;

    @GetMapping
    public ApiResponse<List<JobCriteria>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<JobCriteria> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    public ApiResponse<JobCriteria> create(@Valid @RequestBody CriteriaCreateDTO dto) {
        return ApiResponse.ok(service.create(dto), "Criteria created");
    }

    @PutMapping("/{id}")
    public ApiResponse<JobCriteria> update(@PathVariable UUID id, @Valid @RequestBody CriteriaCreateDTO dto) {
        return ApiResponse.ok(service.update(id, dto), "Criteria updated");
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<JobCriteria> toggle(@PathVariable UUID id) {
        return ApiResponse.ok(service.toggle(id), "Criteria toggled");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Criteria deleted");
    }

    // ---- Boolean criteria builder support (spec §8/§40) ----

    public record QueryTestReq(String query, String sampleText) {}
    public record QueryTestResp(boolean valid, String error, Boolean matches) {}

    /**
     * Validate a boolean criteria expression and (optionally) test it against
     * sample text. Never throws — returns {@code valid=false} with the parse error
     * so the builder can show live feedback.
     */
    @PostMapping("/validate-query")
    public ApiResponse<QueryTestResp> validateQuery(@RequestBody QueryTestReq req) {
        try {
            BooleanQuery q = BooleanQuery.parse(req.query());
            Boolean m = (req.sampleText() != null && !req.sampleText().isBlank())
                    ? q.matches(req.sampleText()) : null;
            return ApiResponse.ok(new QueryTestResp(true, null, m), "Valid expression");
        } catch (RuntimeException e) {
            return ApiResponse.ok(new QueryTestResp(false, e.getMessage(), null), "Invalid expression");
        }
    }
}

