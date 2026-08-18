package com.jobbot.module.role;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.role.dto.TargetRoleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD + priority management for the candidate's target roles (spec §7).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TargetRoleService {

    private final TargetRoleRepository repository;

    public List<TargetRole> getAll() {
        return repository.findAllByOrderByPriorityAsc();
    }

    public List<TargetRole> getActive() {
        return repository.findAllByActiveTrueOrderByPriorityAsc();
    }

    public TargetRole getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Target role not found: " + id));
    }

    @Transactional
    public TargetRole create(TargetRoleDTO dto) {
        if (dto.getRoleTitle() == null || dto.getRoleTitle().isBlank()) {
            throw new JobBotException("roleTitle is required");
        }
        TargetRole role = TargetRole.builder()
                .roleTitle(dto.getRoleTitle())
                .priority(dto.getPriority() != null ? dto.getPriority() : nextPriority())
                .requiredSkills(orEmpty(dto.getRequiredSkills()))
                .preferredSkills(orEmpty(dto.getPreferredSkills()))
                .excludedSkills(orEmpty(dto.getExcludedSkills()))
                .minimumExperience(dto.getMinimumExperience())
                .maximumExperience(dto.getMaximumExperience())
                .locations(orEmpty(dto.getLocations()))
                .remotePreference(dto.getRemotePreference() != null ? dto.getRemotePreference() : "ANY")
                .salaryMinLpa(dto.getSalaryMinLpa())
                .salaryMaxLpa(dto.getSalaryMaxLpa())
                .noticePeriodToleranceDays(dto.getNoticePeriodToleranceDays())
                .active(dto.getActive() == null || dto.getActive())
                .build();
        TargetRole saved = repository.save(role);
        log.info("Created target role '{}' (priority {})", saved.getRoleTitle(), saved.getPriority());
        return saved;
    }

    @Transactional
    public TargetRole update(UUID id, TargetRoleDTO dto) {
        TargetRole role = getById(id);
        if (dto.getRoleTitle() != null) role.setRoleTitle(dto.getRoleTitle());
        if (dto.getPriority() != null) role.setPriority(dto.getPriority());
        if (dto.getRequiredSkills() != null) role.setRequiredSkills(dto.getRequiredSkills());
        if (dto.getPreferredSkills() != null) role.setPreferredSkills(dto.getPreferredSkills());
        if (dto.getExcludedSkills() != null) role.setExcludedSkills(dto.getExcludedSkills());
        if (dto.getMinimumExperience() != null) role.setMinimumExperience(dto.getMinimumExperience());
        if (dto.getMaximumExperience() != null) role.setMaximumExperience(dto.getMaximumExperience());
        if (dto.getLocations() != null) role.setLocations(dto.getLocations());
        if (dto.getRemotePreference() != null) role.setRemotePreference(dto.getRemotePreference());
        if (dto.getSalaryMinLpa() != null) role.setSalaryMinLpa(dto.getSalaryMinLpa());
        if (dto.getSalaryMaxLpa() != null) role.setSalaryMaxLpa(dto.getSalaryMaxLpa());
        if (dto.getNoticePeriodToleranceDays() != null) role.setNoticePeriodToleranceDays(dto.getNoticePeriodToleranceDays());
        if (dto.getActive() != null) role.setActive(dto.getActive());
        return repository.save(role);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new JobBotException("Target role not found: " + id);
        }
        repository.deleteById(id);
    }

    /** Reorder roles: map of roleId → new priority. */
    @Transactional
    public List<TargetRole> reorder(Map<UUID, Integer> priorities) {
        List<TargetRole> updated = new ArrayList<>();
        priorities.forEach((id, priority) -> {
            TargetRole role = getById(id);
            role.setPriority(priority);
            updated.add(repository.save(role));
        });
        return getAll();
    }

    private int nextPriority() {
        return repository.findAllByOrderByPriorityAsc().stream()
                .mapToInt(TargetRole::getPriority).max().orElse(0) + 1;
    }

    private static List<String> orEmpty(List<String> in) {
        return in != null ? in : new ArrayList<>();
    }
}

