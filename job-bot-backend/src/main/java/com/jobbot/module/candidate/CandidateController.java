package com.jobbot.module.candidate;

import com.jobbot.common.ApiResponse;
import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.candidate.dto.ConfirmProfileDTO;
import com.jobbot.module.candidate.dto.ParsedResumeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Phase 1 candidate endpoints (spec §4/§5). All require authentication (PII — spec §49).
 *
 *  POST /api/candidate/resume/parse   multipart → unsaved extraction preview
 *  POST /api/candidate/profile/confirm  persist verified profile
 *  GET  /api/candidate/profile          current verified profile (204 if none)
 *  GET  /api/candidate/skills           verified skills + evidence
 */
@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateProfileService service;

    @PostMapping(value = "/resume/parse", consumes = "multipart/form-data")
    public ApiResponse<ParsedResumeDTO> parse(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JobBotException("No file uploaded");
        }
        try {
            ParsedResumeDTO parsed = service.parse(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
            return ApiResponse.ok(parsed, "Resume parsed — please verify before saving");
        } catch (IOException e) {
            throw new JobBotException("Could not read upload: " + e.getMessage());
        }
    }

    @PostMapping("/profile/confirm")
    public ApiResponse<CandidateProfile> confirm(@RequestBody ConfirmProfileDTO dto) {
        return ApiResponse.ok(service.confirm(dto), "Profile saved");
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CandidateProfile>> get() {
        return service.currentProfile()
                .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/profile")
    public ApiResponse<CandidateProfile> update(@RequestBody ConfirmProfileDTO dto) {
        return ApiResponse.ok(service.confirm(dto), "Profile updated");
    }

    @GetMapping("/skills")
    public ApiResponse<List<CandidateSkill>> skills() {
        return ApiResponse.ok(service.skills());
    }

    @GetMapping("/resume/text")
    public ApiResponse<String> getResumeText() {
        return ApiResponse.ok(service.getLatestResumeText());
    }
}

