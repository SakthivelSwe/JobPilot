package com.jobbot.module.criteria;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.criteria.dto.CriteriaCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CriteriaService {

    private final CriteriaRepository repository;

    public List<JobCriteria> findAll() {
        return repository.findAll();
    }

    public List<JobCriteria> findAllActive() {
        return repository.findAllByActiveTrue();
    }

    public JobCriteria findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Criteria not found: " + id));
    }

    public JobCriteria create(CriteriaCreateDTO dto) {
        JobCriteria c = JobCriteria.builder()
                .name(dto.getName())
                .resumeId(dto.getResumeId())
                .keywords(dto.getKeywords() != null ? dto.getKeywords() : new ArrayList<>())
                .locations(dto.getLocations() != null ? dto.getLocations() : new ArrayList<>())
                .experienceMin(dto.getExperienceMin() != null ? dto.getExperienceMin() : 2)
                .experienceMax(dto.getExperienceMax() != null ? dto.getExperienceMax() : 6)
                .salaryMinLpa(dto.getSalaryMinLpa())
                .salaryMaxLpa(dto.getSalaryMaxLpa())
                .jobType(dto.getJobType() != null ? dto.getJobType() : "permanent")
                .excludeCompanies(dto.getExcludeCompanies() != null ? dto.getExcludeCompanies() : new ArrayList<>())
                .minMatchScore(dto.getMinMatchScore() != null ? dto.getMinMatchScore() : com.jobbot.common.JobPilotThresholds.DEFAULT_MIN_MATCH_SCORE_BD)
                .active(dto.getActive() == null || dto.getActive())
                .build();
        JobCriteria saved = repository.save(c);
        log.info("Created criteria '{}' ({})", saved.getName(), saved.getId());
        return saved;
    }

    public JobCriteria update(UUID id, CriteriaCreateDTO dto) {
        JobCriteria c = findById(id);
        c.setName(dto.getName());
        c.setResumeId(dto.getResumeId());
        if (dto.getKeywords() != null) c.setKeywords(dto.getKeywords());
        if (dto.getLocations() != null) c.setLocations(dto.getLocations());
        if (dto.getExperienceMin() != null) c.setExperienceMin(dto.getExperienceMin());
        if (dto.getExperienceMax() != null) c.setExperienceMax(dto.getExperienceMax());
        c.setSalaryMinLpa(dto.getSalaryMinLpa());
        c.setSalaryMaxLpa(dto.getSalaryMaxLpa());
        if (dto.getJobType() != null) c.setJobType(dto.getJobType());
        if (dto.getExcludeCompanies() != null) c.setExcludeCompanies(dto.getExcludeCompanies());
        if (dto.getMinMatchScore() != null) c.setMinMatchScore(dto.getMinMatchScore());
        if (dto.getActive() != null) c.setActive(dto.getActive());
        return repository.save(c);
    }

    public JobCriteria toggle(UUID id) {
        JobCriteria c = findById(id);
        c.setActive(!c.isActive());
        return repository.save(c);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new JobBotException("Criteria not found: " + id);
        }
        repository.deleteById(id);
    }
}

