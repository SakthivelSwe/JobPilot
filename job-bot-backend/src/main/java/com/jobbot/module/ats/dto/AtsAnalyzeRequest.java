package com.jobbot.module.ats.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class AtsAnalyzeRequest {
    private UUID resumeId;
    @NotBlank
    private String jobDescription;
}

