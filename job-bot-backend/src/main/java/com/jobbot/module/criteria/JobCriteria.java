package com.jobbot.module.criteria;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_criteria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCriteria {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> locations = new ArrayList<>();

    @Column(name = "experience_min")
    @Builder.Default
    private int experienceMin = 2;

    @Column(name = "experience_max")
    @Builder.Default
    private int experienceMax = 6;

    @Column(name = "salary_min_lpa")
    private BigDecimal salaryMinLpa;

    @Column(name = "salary_max_lpa")
    private BigDecimal salaryMaxLpa;

    @Column(name = "job_type")
    @Builder.Default
    private String jobType = "permanent";

    @Convert(converter = StringListConverter.class)
    @Column(name = "exclude_companies", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> excludeCompanies = new ArrayList<>();

    @Column(name = "min_match_score")
    @Builder.Default
    private BigDecimal minMatchScore = BigDecimal.valueOf(65);

    /**
     * Optional boolean criteria expression (spec §8/§40), e.g.
     * "Java AND Spring Boot AND (Kafka OR Microservices) AND NOT Intern".
     * Parsed/evaluated by {@code com.jobbot.module.criteria.expression.BooleanQuery}.
     */
    @Column(name = "boolean_query", columnDefinition = "TEXT")
    private String booleanQuery;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

