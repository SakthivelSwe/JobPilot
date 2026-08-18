package com.jobbot.module.matching;

/**
 * Normalized work-mode (spec §11/§17). {@link #UNKNOWN} is used whenever the
 * source text is ambiguous — we never guess (spec §12).
 */
public enum WorkMode {
    REMOTE,
    HYBRID,
    ONSITE,
    UNKNOWN
}

