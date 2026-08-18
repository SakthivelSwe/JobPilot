package com.jobbot.module.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfigUpdateDTO {
    private Boolean enabled;
    private Integer dailyLimit;
    private Integer minDelaySeconds;
    // Credential fields removed (spec §1): JobPilot never stores platform account
    // credentials or session cookies.
}

