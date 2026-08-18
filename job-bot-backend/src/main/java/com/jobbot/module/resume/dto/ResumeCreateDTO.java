package com.jobbot.module.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ResumeCreateDTO {

    @NotBlank
    private String name;
    private String fileUrl;
    private List<String> targetRoles;
    private List<String> targetSkills;
    private String resumeText;
    private String experienceSummary;
    private Boolean active;
}

