package com.jobbot.module.job.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class JobImportDTO {
    private String platform;        // linkedin | naukri | indeed | other
    private String platformJobId;
    private String title;
    private String company;
    private String location;
    private String description;     // paste the JD text here
    private String url;
    private String salaryRange;
    private String experienceRequired;
    /** Optional: score immediately against this criteria's resume. */
    private UUID criteriaId;
}

