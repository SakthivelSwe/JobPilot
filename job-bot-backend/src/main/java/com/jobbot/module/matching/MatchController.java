package com.jobbot.module.matching;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateProfileService;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Matching API (spec §14/§15/§32). Scores discovered postings against the verified
 * candidate profile and returns ranked opportunities. Read-only, deterministic.
 */
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final JobMatchService matchService;
    private final CandidateProfileService profileService;
    private final JobPostingRepository postingRepository;

    /** Full 8-factor breakdown for one posting vs the current profile. */
    @GetMapping("/posting/{id}")
    public ApiResponse<RankedMatch> matchPosting(@PathVariable UUID id) {
        CandidateProfile profile = profileService.getOrThrow();
        JobPosting posting = postingRepository.findById(id)
                .orElseThrow(() -> new com.jobbot.common.exception.JobBotException("Posting not found: " + id));
        return ApiResponse.ok(new RankedMatch(posting, matchService.match(profile, posting)));
    }

    /**
     * Top opportunities: score the most recent {@code scanSize} postings and return the
     * best {@code limit} by overall score (spec §32 "New matches today").
     */
    @GetMapping("/top")
    public ApiResponse<List<RankedMatch>> top(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "200") int scanSize) {
        CandidateProfile profile = profileService.getOrThrow();
        var page = postingRepository.findAll(
                PageRequest.of(0, Math.min(scanSize, 500), Sort.by(Sort.Direction.DESC, "createdAt")));
        List<RankedMatch> ranked = page.getContent().stream()
                .map(p -> new RankedMatch(p, matchService.match(profile, p)))
                .sorted(Comparator.comparingInt((RankedMatch r) -> r.match().overallScore()).reversed())
                .limit(Math.max(1, limit))
                .toList();
        return ApiResponse.ok(ranked);
    }

    /**
     * Recompute and persist match score + recommendation for stored postings
     * (call after the candidate profile changes). Bounded for safety.
     */
    @PostMapping("/rescore")
    public ApiResponse<java.util.Map<String, Object>> rescore(
            @RequestParam(defaultValue = "1000") int max) {
        CandidateProfile profile = profileService.getOrThrow();
        var page = postingRepository.findAll(
                PageRequest.of(0, Math.min(max, 5000), Sort.by(Sort.Direction.DESC, "createdAt")));
        int updated = 0;
        for (JobPosting p : page.getContent()) {
            MatchResult m = matchService.match(profile, p);
            p.setMatchScore(m.overallScore());
            p.setRecommendation(m.recommendation().name());
            postingRepository.save(p);
            updated++;
        }
        return ApiResponse.ok(java.util.Map.of("rescored", updated), updated + " postings rescored");
    }
}

