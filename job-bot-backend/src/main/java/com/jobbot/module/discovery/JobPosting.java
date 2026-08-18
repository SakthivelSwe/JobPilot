package com.jobbot.module.discovery;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A normalized, deduplicated job posting discovered from an authorized source
 * (spec §11). All list fields use the portable {@link StringListConverter}.
 */
@Entity
@Table(name = "job_posting",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"}),
        indexes = {
                @Index(name = "idx_posting_hash", columnList = "normalized_hash"),
                @Index(name = "idx_posting_status", columnList = "status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Source/ATS the posting was discovered from. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AtsType source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "application_url", columnDefinition = "TEXT")
    private String applicationUrl;

    @Column(nullable = false)
    private String title;

    private String company;

    @Column(name = "company_id")
    private UUID companyId;

    private String location;
    private String country;

    /** REMOTE / HYBRID / ONSITE / UNKNOWN (from {@code WorkMode}). */
    @Column(name = "remote_type")
    private String remoteType;

    /** FULL_TIME / PART_TIME / CONTRACT / INTERNSHIP / UNKNOWN. */
    @Column(name = "employment_type")
    @Builder.Default
    private String employmentType = "UNKNOWN";

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal salary;
    private String currency;

    @Column(name = "minimum_experience")
    private Integer minimumExperience;

    @Column(name = "maximum_experience")
    private Integer maximumExperience;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Convert(converter = StringListConverter.class)
    @Column(name = "required_skills", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> responsibilities = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> qualifications = new ArrayList<>();

    /** Deduplication key: normalized company|title|location hash (spec §13). */
    @Column(name = "normalized_hash")
    private String normalizedHash;

    /** Other sources this same job was also seen on (spec §13 source history). */
    @Convert(converter = StringListConverter.class)
    @Column(name = "sources_seen", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> sourcesSeen = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "application_capability")
    @Builder.Default
    private ApplicationCapability applicationCapability = ApplicationCapability.MANUAL_REQUIRED;

    /** Deterministic match score vs the candidate profile (0–100), computed at discovery (§14). */
    @Column(name = "match_score")
    private Integer matchScore;

    /** STRONG_APPLY / APPLY / REVIEW / LOW_PRIORITY / SKIP (§15). */
    @Column(name = "recommendation")
    private String recommendation;

    /** DISCOVERED / MATCHED / ... (posting lifecycle). */
    @Builder.Default
    private String status = "DISCOVERED";

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

