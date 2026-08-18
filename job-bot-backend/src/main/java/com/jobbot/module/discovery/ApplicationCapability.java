package com.jobbot.module.discovery;

/**
 * How a discovered job can be applied to (spec §2/§24).
 * Default is always the SAFE option — never assume AUTO just because an ATS is known.
 */
public enum ApplicationCapability {
    /** Programmatic submission via an explicitly authorized API + user opt-in (§25). */
    AUTO_ELIGIBLE,
    /** JobPilot prepares the full pack; the user performs the final submit (§2). */
    ASSISTED_APPLY,
    /** Valid & relevant, but JobPilot cannot legitimately automate — user applies (§2/§26). */
    MANUAL_REQUIRED,
    /** No longer accessible, closed, broken, or duplicate (§2). */
    UNAVAILABLE
}

