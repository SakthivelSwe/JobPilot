package com.jobbot.module.manualqueue;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A high-quality job that needs the user's final manual submission (spec §27/§42).
 * This is the compliant replacement for the removed auto-apply queue — nothing here
 * ever submits an application automatically.
 */
@Entity
@Table(name = "manual_queue_entry",
        uniqueConstraints = @UniqueConstraint(columnNames = "posting_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "posting_id", nullable = false)
    private UUID postingId;

    private String company;
    private String role;
    private String source;

    @Column(name = "job_url", columnDefinition = "TEXT")
    private String jobUrl;

    @Column(name = "application_url", columnDefinition = "TEXT")
    private String applicationUrl;

    /** ASSISTED_APPLY / MANUAL_REQUIRED / ... (from the posting's capability). */
    @Column(name = "capability")
    private String capability;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "match_score")
    private int matchScore;

    @Column(name = "recommended_variant")
    private String recommendedVariant;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ManualQueueStatus status = ManualQueueStatus.PENDING;

    /** Set once the user marks it applied and it flows into the CRM. */
    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

