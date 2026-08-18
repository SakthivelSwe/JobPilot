package com.jobbot.module.job;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.ats.AtsService;
import com.jobbot.module.ats.dto.AtsResult;
import com.jobbot.module.criteria.CriteriaService;
import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.job.dto.JobImportDTO;
import com.jobbot.module.resume.Resume;
import com.jobbot.module.resume.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository repository;
    private final AtsService atsService;
    private final CriteriaService criteriaService;
    private final ResumeService resumeService;

    public Page<Job> findAll(String status, String platform, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return repository.findByStatus(status, pageable);
        }
        if (platform != null && !platform.isBlank()) {
            return repository.findByPlatform(platform, pageable);
        }
        return repository.findAll(pageable);
    }

    public Job findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Job not found: " + id));
    }

    /** Import a manually-copied job; optionally score it immediately. */
    public Job importJob(JobImportDTO dto) {
        String platform = dto.getPlatform() != null ? dto.getPlatform() : "other";
        String platformJobId = dto.getPlatformJobId() != null ? dto.getPlatformJobId()
                : (dto.getUrl() != null ? dto.getUrl() : UUID.randomUUID().toString());

        Job job = repository.findByPlatformAndPlatformJobId(platform, platformJobId)
                .orElseGet(Job::new);

        job.setPlatform(platform);
        job.setPlatformJobId(platformJobId);
        job.setTitle(dto.getTitle());
        job.setCompany(dto.getCompany());
        job.setLocation(dto.getLocation());
        job.setDescription(dto.getDescription());
        job.setUrl(dto.getUrl());
        job.setSalaryRange(dto.getSalaryRange());
        job.setExperienceRequired(dto.getExperienceRequired());
        job.setCriteriaId(dto.getCriteriaId());
        if (job.getStatus() == null) job.setStatus("new");

        Job saved = repository.save(job);
        log.info("Imported job '{}' at '{}'", saved.getTitle(), saved.getCompany());

        if (dto.getCriteriaId() != null) {
            saved = scoreAgainstCriteria(saved.getId(), dto.getCriteriaId());
        }
        return saved;
    }

    /** Run the deterministic ATS engine using the criteria's linked resume. */
    public Job scoreAgainstCriteria(UUID jobId, UUID criteriaId) {
        Job job = findById(jobId);
        JobCriteria criteria = criteriaService.findById(criteriaId);

        String resumeText = null;
        List<String> skills = criteria.getKeywords();
        if (criteria.getResumeId() != null) {
            Resume resume = resumeService.findById(criteria.getResumeId());
            resumeText = resume.getResumeText();
            if (resume.getTargetSkills() != null && !resume.getTargetSkills().isEmpty()) {
                skills = resume.getTargetSkills();
            }
        }

        AtsResult ats = atsService.analyze(
                resumeText, job.getDescription(), skills,
                criteria.getLocations(), criteria.getExperienceMin(), criteria.getExperienceMax());

        return applyAtsResult(job, ats, criteria.getMinMatchScore());
    }

    /** Score a job against a specific resume (ad-hoc). */
    public Job scoreAgainstResume(UUID jobId, UUID resumeId) {
        Job job = findById(jobId);
        Resume resume = resumeService.findById(resumeId);
        AtsResult ats = atsService.analyzeJobFit(
                resume.getResumeText(), job.getDescription(), resume.getTargetSkills());
        return applyAtsResult(job, ats, com.jobbot.common.JobPilotThresholds.DEFAULT_MIN_MATCH_SCORE_BD);
    }

    private Job applyAtsResult(Job job, AtsResult ats, BigDecimal minScore) {
        job.setMatchScore(BigDecimal.valueOf(ats.getScore()));
        job.setMatchKeywords(ats.getMatchedKeywords());
        job.setMissingKeywords(ats.getMissingKeywords());
        job.setReasonToApply(ats.getReasonToApply());
        BigDecimal threshold = minScore != null ? minScore
                : com.jobbot.common.JobPilotThresholds.DEFAULT_MIN_MATCH_SCORE_BD;
        job.setStatus(BigDecimal.valueOf(ats.getScore()).compareTo(threshold) >= 0 ? "matched" : "skipped");
        return repository.save(job);
    }

    public Job updateStatus(UUID id, String status) {
        Job job = findById(id);
        job.setStatus(status);
        return repository.save(job);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new JobBotException("Job not found: " + id);
        }
        repository.deleteById(id);
    }
}

