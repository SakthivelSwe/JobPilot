package com.jobbot.module.matching;

/**
 * Outcome of the deterministic location comparison (spec §17).
 * Ordered best→worst so callers can compare ordinals for ranking.
 */
public enum LocationMatch {
    EXACT,        // same normalized city (or remote↔remote)
    REMOTE,       // job is remote and candidate accepts remote
    CITY,         // one of the candidate's preferred cities
    COUNTRY,      // same country, different city
    RELOCATION,   // not preferred, but candidate is open to relocation
    UNKNOWN,      // job location could not be determined
    NONE          // no compatibility found
}

