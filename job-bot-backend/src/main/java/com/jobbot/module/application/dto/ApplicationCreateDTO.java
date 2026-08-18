package com.jobbot.module.application.dto;

import lombok.Data;

import java.util.UUID;

/** Create an application from a job the user has (manually) applied to. */
@Data
public class ApplicationCreateDTO {
    private UUID jobId;
    private UUID resumeId;
    private UUID criteriaId;
    private String coverLetter;
    private String notes;
}

