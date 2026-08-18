package com.jobbot.module.resume.variant;

import com.jobbot.common.ApiResponse;
import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateProfileService;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Four-resume engine API (spec §19–21): list variants, auto-select the best variant
 * for a posting, and produce a tailored resume from verified facts.
 */
@RestController
@RequestMapping("/api/resume-engine")
@RequiredArgsConstructor
public class ResumeEngineController {

    private final ResumeSelectionService selectionService;
    private final ResumeTailoringService tailoringService;
    private final CandidateProfileService profileService;
    private final JobPostingRepository postingRepository;

    public record VariantInfo(ResumeVariant variant, String roleTarget, List<String> prioritySkills) {}

    /** The four canonical variants and their emphasis (spec §19). */
    @GetMapping("/variants")
    public ApiResponse<List<VariantInfo>> variants() {
        List<VariantInfo> list = java.util.Arrays.stream(ResumeVariant.values())
                .map(v -> new VariantInfo(v, v.roleTarget(), v.prioritySkills()))
                .toList();
        return ApiResponse.ok(list);
    }

    /** Rank the four variants for a posting; the best is flagged recommended (spec §20). */
    @GetMapping("/select/{postingId}")
    public ApiResponse<List<VariantScore>> select(@PathVariable UUID postingId) {
        return ApiResponse.ok(selectionService.rank(requirePosting(postingId)));
    }

    /**
     * Produce a tailored resume for a posting. If {@code variant} is omitted the best
     * variant is auto-selected (spec §20/§21).
     */
    @GetMapping("/tailor/{postingId}")
    public ApiResponse<TailoredResume> tailor(@PathVariable UUID postingId,
                                              @RequestParam(required = false) String variant) {
        CandidateProfile profile = profileService.getOrThrow();
        JobPosting posting = requirePosting(postingId);
        ResumeVariant chosen = (variant != null && !variant.isBlank())
                ? parseVariant(variant)
                : selectionService.selectBest(posting).variant();
        return ApiResponse.ok(tailoringService.tailor(profile, posting, chosen),
                "Tailored using " + chosen.roleTarget());
    }

    private JobPosting requirePosting(UUID id) {
        return postingRepository.findById(id)
                .orElseThrow(() -> new JobBotException("Posting not found: " + id));
    }

    private ResumeVariant parseVariant(String raw) {
        try {
            return ResumeVariant.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new JobBotException("Unknown resume variant: " + raw);
        }
    }
}

