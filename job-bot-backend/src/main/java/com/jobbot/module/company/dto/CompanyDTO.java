package com.jobbot.module.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {
    private String name;
    private String domain;
    private String careersUrl;
    private String country;
    private String industry;
    private String companyType;
    /** GREENHOUSE / ASHBY / MANUAL / ... */
    private String atsType;
    private String atsToken;
    private Boolean active;
}

