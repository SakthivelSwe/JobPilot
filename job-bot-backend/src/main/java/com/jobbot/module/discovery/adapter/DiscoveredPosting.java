package com.jobbot.module.discovery.adapter;

import com.jobbot.module.discovery.AtsType;

import java.time.LocalDate;

/**
 * Raw posting as fetched from a source adapter, before normalization (spec §9→§12).
 */
public record DiscoveredPosting(
        AtsType source,
        String externalId,
        String title,
        String company,
        String location,
        String jobUrl,
        String applicationUrl,
        String description,
        String employmentTypeRaw,
        String remoteHint,
        LocalDate postingDate
) {}

