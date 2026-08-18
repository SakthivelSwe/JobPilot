package com.jobbot.module.matching;

/**
 * Notice-period compatibility outcome (spec §16). Displayed as:
 * ✓ Compatible / △ Recruiter approval required / ✗ Major mismatch.
 */
public enum NoticeCompatibility {
    COMPATIBLE,          // candidate can join within the job's window
    RECRUITER_APPROVAL,  // slightly over the window — negotiable
    MAJOR_MISMATCH,      // well beyond the window
    UNKNOWN              // job requirement not specified
}

