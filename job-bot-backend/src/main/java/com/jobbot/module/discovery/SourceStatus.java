package com.jobbot.module.discovery;

/**
 * Health of a configured job source (spec §57). Never claim a source is active
 * when it is only manually supported.
 */
public enum SourceStatus {
    HEALTHY,       // last scan succeeded
    DEGRADED,      // last scan partially failed
    UNAVAILABLE,   // last scan failed / endpoint unreachable
    MANUAL         // supported only via manual import (e.g. LinkedIn, Naukri)
}

