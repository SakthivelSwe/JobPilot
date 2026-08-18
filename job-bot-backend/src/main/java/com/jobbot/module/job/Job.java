package com.jobbot.module.job;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String platform;

    @Column(name = "platform_job_id")
    private String platformJobId;

    private String title;
    private String company;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "salary_range")
    private String salaryRange;

    @Column(name = "experience_required")
    private String experienceRequired;

    @Column(name = "posted_date")
    private OffsetDateTime postedDate;

    @Column(name = "scraped_at")
    private OffsetDateTime scrapedAt;

    @Column(name = "criteria_id")
    private UUID criteriaId;

    @Column(name = "match_score")
    private BigDecimal matchScore;

    @Convert(converter = StringListConverter.class)
    @Column(name = "match_keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> matchKeywords = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "missing_keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> missingKeywords = new ArrayList<>();

    @Column(name = "reason_to_apply", columnDefinition = "TEXT")
    private String reasonToApply;

    @Builder.Default
    private String status = "new";

    @Column(name = "source_queue_id")
    private UUID sourceQueueId;

    @PrePersist
    void onCreate() {
        if (scrapedAt == null) scrapedAt = OffsetDateTime.now();
    }
}

