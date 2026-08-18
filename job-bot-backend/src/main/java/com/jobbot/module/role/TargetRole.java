package com.jobbot.module.role;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A prioritized target role the candidate is pursuing (spec §7).
 * Multiple roles can exist, each with its own skill requirements and constraints.
 * All {@code List<String>} fields use the portable {@link StringListConverter}
 * (JSON-in-TEXT) so the schema works identically on H2 and PostgreSQL (spec §50).
 */
@Entity
@Table(name = "target_role")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetRole {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "role_title", nullable = false)
    private String roleTitle;

    /** 1 = highest priority. Lower number ranks first. */
    @Column(name = "priority")
    @Builder.Default
    private int priority = 1;

    @Convert(converter = StringListConverter.class)
    @Column(name = "required_skills", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "excluded_skills", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> excludedSkills = new ArrayList<>();

    @Column(name = "minimum_experience")
    private Integer minimumExperience;

    @Column(name = "maximum_experience")
    private Integer maximumExperience;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> locations = new ArrayList<>();

    /** REMOTE / HYBRID / ONSITE / ANY */
    @Column(name = "remote_preference")
    @Builder.Default
    private String remotePreference = "ANY";

    @Column(name = "salary_min_lpa")
    private BigDecimal salaryMinLpa;

    @Column(name = "salary_max_lpa")
    private BigDecimal salaryMaxLpa;

    @Column(name = "notice_period_tolerance_days")
    private Integer noticePeriodToleranceDays;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

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

