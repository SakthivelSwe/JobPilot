package com.jobbot.module.discovery;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Discovery API (spec §9, §56–58). Manual scan, source health, coverage, and the
 * normalized posting feed. No auto-apply — discovery only prepares & ranks.
 */
@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final JobDiscoveryService discoveryService;
    private final SourceHealthService sourceHealthService;
    private final JobPostingRepository postingRepository;

    /** Trigger a manual scan across all active authorized sources (spec §56). */
    @PostMapping("/scan")
    public ApiResponse<DiscoveryResult> scan() {
        DiscoveryResult result = discoveryService.scan();
        return ApiResponse.ok(result,
                result.newPostings() + " new postings from " + result.companiesScanned() + " sources");
    }

    /** Source health table (spec §57). */
    @GetMapping("/sources")
    public ApiResponse<List<SourceHealthService.SourceHealthRow>> sources() {
        return ApiResponse.ok(sourceHealthService.health());
    }

    /** Coverage dashboard stats (spec §58). */
    @GetMapping("/coverage")
    public ApiResponse<SourceHealthService.CoverageStats> coverage() {
        return ApiResponse.ok(sourceHealthService.coverage());
    }

    /** Normalized, deduplicated postings feed (spec §32). */
    @GetMapping("/postings")
    public ApiResponse<Page<JobPosting>> postings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobPosting> result = (status != null && !status.isBlank())
                ? postingRepository.findByStatus(status, pr)
                : postingRepository.findAll(pr);
        return ApiResponse.ok(result);
    }
}

