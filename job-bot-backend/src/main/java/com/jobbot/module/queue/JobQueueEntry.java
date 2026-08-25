package com.jobbot.module.queue;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A discovered posting that has been enqueued for user review / auto-apply.
 *
 * <p>Distinct from {@code JobPosting}: JobPosting is the normalized discovery record;
 * JobQueueEntry is the actionable review/apply pipeline row. One posting can be in
 * the queue at most once per (external_id, platform).
 */
@Entity
@Table(name = "job_queue",
        indexes = {
                @Index(name = "idx_queue_status", columnList = "status"),
                @Index(name = "idx_queue_platform_status", columnList = "platform,status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobQueueEntry {

    @org.hibernate.annotations.TenantId
    @Column(name = "user_id")
    private String userId;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_posting_id")
    private UUID jobPostingId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    /** NAUKRI | LINKEDIN | INDEED | GREENHOUSE ... */
    @Column(nullable = false, length = 50)
    private String platform;

    private String title;
    private String company;
    private String location;

    @Column(name = "job_url", columnDefinition = "TEXT")
    private String jobUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ats_score", precision = 5, scale = 2)
    private BigDecimal atsScore;

    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(length = 50)
    private String recommendation;

    @Convert(converter = StringListConverter.class)
    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "missing_keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> missingKeywords = new ArrayList<>();

    /** Recommended ResumeVariant name (e.g. FULL_STACK). */
    @Column(name = "resume_variant", length = 50)
    private String resumeVariant;

    @Column(name = "criteria_id")
    private UUID criteriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private JobQueueStatus status = JobQueueStatus.PENDING_REVIEW;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

