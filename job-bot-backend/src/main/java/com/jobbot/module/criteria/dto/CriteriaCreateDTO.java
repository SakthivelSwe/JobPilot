package com.jobbot.module.criteria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CriteriaCreateDTO {

    @NotBlank
    private String name;
    private UUID resumeId;
    private List<String> keywords;
    private List<String> locations;
    private Integer experienceMin;
    private Integer experienceMax;
    private BigDecimal salaryMinLpa;
    private BigDecimal salaryMaxLpa;
    private String jobType;
    private List<String> excludeCompanies;
    private BigDecimal minMatchScore;
    private Boolean active;
}

