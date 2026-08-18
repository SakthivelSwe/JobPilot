package com.jobbot.module.company;

import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.SourceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A monitored employer and its public career source (spec §10).
 * {@code atsToken} is the public board identifier (Greenhouse board token or
 * Ashby org slug) used by the discovery adapters.
 */
@Entity
@Table(name = "company",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ats_type", "ats_token"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String domain;

    @Column(name = "careers_url")
    private String careersUrl;

    private String country;

    private String industry;

    /** PRODUCT / SERVICE / STARTUP / ENTERPRISE (free-form). */
    @Column(name = "company_type")
    private String companyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ats_type")
    @Builder.Default
    private AtsType atsType = AtsType.MANUAL;

    /** Public board identifier: Greenhouse board token or Ashby org slug. */
    @Column(name = "ats_token")
    private String atsToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_status")
    @Builder.Default
    private SourceStatus sourceStatus = SourceStatus.HEALTHY;

    @Column(name = "last_checked")
    private OffsetDateTime lastChecked;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

