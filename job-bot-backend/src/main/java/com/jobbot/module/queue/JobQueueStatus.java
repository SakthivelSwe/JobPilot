package com.jobbot.module.queue;

/**
 * Lifecycle status for a {@link JobQueueEntry}.
 *
 * <pre>
 * PENDING_REVIEW → APPROVED → AUTO_APPLYING → APPLIED
 *                     ↓            ↓
 *                   SKIPPED    FAILED_APPLY / MANUAL_APPLY
 * </pre>
 */
public enum JobQueueStatus {
    PENDING_REVIEW,
    APPROVED,
    AUTO_APPLYING,
    APPLIED,
    FAILED_APPLY,
    MANUAL_APPLY,
    SKIPPED,
    FILTERED_OUT
}

