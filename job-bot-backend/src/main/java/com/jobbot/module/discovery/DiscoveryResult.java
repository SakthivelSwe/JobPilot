package com.jobbot.module.discovery;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Outcome of a discovery scan (spec §9/§56/§58).
 */
public record DiscoveryResult(
        int companiesScanned,
        int totalFound,
        int newPostings,
        int crossSourceDuplicates,
        int alreadySeen,
        int errors,
        OffsetDateTime scannedAt,
        List<SourceOutcome> sourceOutcomes
) {
    public record SourceOutcome(String company, AtsType source, SourceStatus status,
                                int found, int added, String error) {}
}

