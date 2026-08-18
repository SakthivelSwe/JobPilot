package com.jobbot.module.candidate;

/**
 * Skill proficiency. Deliberately conservative — the parser must NEVER auto-assign
 * EXPERT (spec §5/§6). Default for parsed-but-unverified skills is UNKNOWN.
 */
public enum Proficiency {
    LEARNING,
    BEGINNER,
    WORKING,
    STRONG,
    EXPERT,
    UNKNOWN
}

