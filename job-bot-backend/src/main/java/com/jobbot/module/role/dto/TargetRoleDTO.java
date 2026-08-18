package com.jobbot.module.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetRoleDTO {
    private String roleTitle;
    private Integer priority;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private List<String> excludedSkills;
    private Integer minimumExperience;
    private Integer maximumExperience;
    private List<String> locations;
    private String remotePreference;
    private BigDecimal salaryMinLpa;
    private BigDecimal salaryMaxLpa;
    private Integer noticePeriodToleranceDays;
    private Boolean active;
}

