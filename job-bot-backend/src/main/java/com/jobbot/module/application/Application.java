package com.jobbot.module.application;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(name = "criteria_id")
    private UUID criteriaId;

    private String platform;

    // Denormalized for quick display / already-applied checks
    private String company;
    private String title;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @Builder.Default
    private String status = "applied";

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "ats_score")
    private BigDecimal atsScore;

    @Convert(converter = StringListConverter.class)
    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "missing_keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> missingKeywords = new ArrayList<>();

    @Column(name = "recruiter_name")
    private String recruiterName;

    @Column(name = "recruiter_contact")
    private String recruiterContact;

    @Column(name = "interview_date")
    private OffsetDateTime interviewDate;

    @Column(name = "interview_round")
    @Builder.Default
    private int interviewRound = 0;

    @Column(name = "offer_ctc_lpa")
    private BigDecimal offerCtcLpa;

    @Column(name = "offer_details", columnDefinition = "TEXT")
    private String offerDetails;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;

    @Column(name = "auto_applied")
    @Builder.Default
    private boolean autoApplied = false;

    @Column(name = "job_queue_id")
    private UUID jobQueueId;

    @PrePersist
    void onCreate() {
        if (appliedAt == null) appliedAt = OffsetDateTime.now();
        lastUpdated = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        lastUpdated = OffsetDateTime.now();
    }
}

