package com.jobbot.module.resume;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.resume.dto.ResumeCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository repository;

    public List<Resume> findAll() {
        return repository.findAll();
    }

    public List<Resume> findAllActive() {
        return repository.findAllByActiveTrue();
    }

    public Resume findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Resume not found: " + id));
    }

    public Resume create(ResumeCreateDTO dto) {
        Resume resume = Resume.builder()
                .name(dto.getName())
                .fileUrl(dto.getFileUrl())
                .targetRoles(dto.getTargetRoles() != null ? dto.getTargetRoles() : new ArrayList<>())
                .targetSkills(dto.getTargetSkills() != null ? dto.getTargetSkills() : new ArrayList<>())
                .resumeText(dto.getResumeText())
                .experienceSummary(dto.getExperienceSummary())
                .active(dto.getActive() == null || dto.getActive())
                .build();
        Resume saved = repository.save(resume);
        log.info("Created resume '{}' ({})", saved.getName(), saved.getId());
        return saved;
    }

    public Resume update(UUID id, ResumeCreateDTO dto) {
        Resume resume = findById(id);
        resume.setName(dto.getName());
        resume.setFileUrl(dto.getFileUrl());
        if (dto.getTargetRoles() != null) resume.setTargetRoles(dto.getTargetRoles());
        if (dto.getTargetSkills() != null) resume.setTargetSkills(dto.getTargetSkills());
        resume.setResumeText(dto.getResumeText());
        resume.setExperienceSummary(dto.getExperienceSummary());
        if (dto.getActive() != null) resume.setActive(dto.getActive());
        Resume saved = repository.save(resume);
        log.info("Updated resume {}", id);
        return saved;
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new JobBotException("Resume not found: " + id);
        }
        repository.deleteById(id);
        log.info("Deleted resume {}", id);
    }
}

