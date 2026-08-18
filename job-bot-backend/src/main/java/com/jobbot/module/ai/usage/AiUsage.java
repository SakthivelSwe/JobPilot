package com.jobbot.module.ai.usage;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-day, per-provider, per-feature AI usage counter (spec §55). Enables a daily cap
 * and cost protection. Deterministic engines run first; AI is optional on top.
 */
@Entity
@Table(name = "ai_usage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usage_date", "provider", "feature"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String feature;

    @Builder.Default
    private int requests = 0;

    @Column(name = "estimated_tokens")
    @Builder.Default
    private long estimatedTokens = 0;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }
}

