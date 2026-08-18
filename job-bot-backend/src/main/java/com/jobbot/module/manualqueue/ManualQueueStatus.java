package com.jobbot.module.manualqueue;

/** Lifecycle of a manual-application queue entry (spec §27/§42). */
public enum ManualQueueStatus {
    PENDING,   // waiting for the user to apply
    OPENED,    // user opened the application page
    APPLIED,   // user marked it applied → carried into the Kanban CRM
    SKIPPED    // user decided to skip
}

