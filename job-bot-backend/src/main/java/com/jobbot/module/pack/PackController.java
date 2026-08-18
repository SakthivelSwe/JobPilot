package com.jobbot.module.pack;

import com.jobbot.common.ApiResponse;
import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.ats.AtsService;
import com.jobbot.module.ats.dto.AtsResult;
import com.jobbot.module.ats.dto.ResumeMatch;
import com.jobbot.module.job.Job;
import com.jobbot.module.job.JobService;
import com.jobbot.module.resume.Resume;
import com.jobbot.module.resume.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application Pack endpoints: pick the best resume for a job and generate a
 * tailored cover letter — the value-add of the human-in-the-loop workflow.
 */
@RestController
@RequestMapping("/api/pack")
@RequiredArgsConstructor
public class PackController {

    private final AtsService atsService;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final CoverLetterService coverLetterService;
    private final AnswersService answersService;

    /** Rank all active resumes against a job (by id or raw description). */
    @PostMapping("/best-resume")
    public ApiResponse<List<ResumeMatch>> bestResume(@RequestBody Map<String, String> body) {
        String jd = resolveJd(body);
        List<Resume> resumes = resumeService.findAllActive();
        if (resumes.isEmpty()) {
            throw new JobBotException("No active resumes to compare. Create a resume first.");
        }
        return ApiResponse.ok(atsService.rankResumes(jd, resumes));
    }

    /** Generate a tailored cover letter for a job + resume. */
    @PostMapping("/cover-letter")
    public ApiResponse<Map<String, String>> coverLetter(@RequestBody Map<String, String> body) {
        String jobId = body.get("jobId");
        String resumeId = body.get("resumeId");
        if (jobId == null || resumeId == null) {
            throw new JobBotException("jobId and resumeId are required");
        }
        Job job = jobService.findById(UUID.fromString(jobId));
        Resume resume = resumeService.findById(UUID.fromString(resumeId));
        AtsResult ats = atsService.analyzeJobFit(
                resume.getResumeText(), job.getDescription(), resume.getTargetSkills());
        String letter = coverLetterService.generate(job, resume, ats);
        return ApiResponse.ok(Map.of("coverLetter", letter));
    }

    /** Suggested answers to common screening questions for a job + resume. */
    @PostMapping("/answers")
    public ApiResponse<List<Map<String, String>>> answers(@RequestBody Map<String, String> body) {
        String jobId = body.get("jobId");
        String resumeId = body.get("resumeId");
        if (jobId == null || resumeId == null) {
            throw new JobBotException("jobId and resumeId are required");
        }
        Job job = jobService.findById(UUID.fromString(jobId));
        Resume resume = resumeService.findById(UUID.fromString(resumeId));
        AtsResult ats = atsService.analyzeJobFit(
                resume.getResumeText(), job.getDescription(), resume.getTargetSkills());
        return ApiResponse.ok(answersService.generate(job, resume, ats));
    }

    private String resolveJd(Map<String, String> body) {
        String jobId = body.get("jobId");
        if (jobId != null && !jobId.isBlank()) {
            return jobService.findById(UUID.fromString(jobId)).getDescription();
        }
        String jd = body.get("jobDescription");
        if (jd == null || jd.isBlank()) {
            throw new JobBotException("Provide jobId or jobDescription");
        }
        return jd;
    }
}

