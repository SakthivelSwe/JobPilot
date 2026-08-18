package com.jobbot.module.ats;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.ats.dto.AtsAnalyzeRequest;
import com.jobbot.module.ats.dto.AtsResult;
import com.jobbot.module.resume.Resume;
import com.jobbot.module.resume.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ats")
@RequiredArgsConstructor
public class AtsController {

    private final AtsService atsService;
    private final ResumeService resumeService;

    @PostMapping("/analyze")
    public ApiResponse<AtsResult> analyze(@Valid @RequestBody AtsAnalyzeRequest req) {
        String resumeText = null;
        List<String> skills = List.of();
        List<String> locations = List.of();
        if (req.getResumeId() != null) {
            Resume resume = resumeService.findById(req.getResumeId());
            resumeText = resume.getResumeText();
            skills = resume.getTargetSkills();
        }
        AtsResult result = atsService.analyze(resumeText, req.getJobDescription(), skills, locations, 0, 0);
        return ApiResponse.ok(result);
    }
}

