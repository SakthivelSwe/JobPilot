package com.jobbot.module.platform;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_config",
        uniqueConstraints = @UniqueConstraint(columnNames = "platform_name"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfig {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** NAUKRI | LINKEDIN | INDEED */
    @Column(name = "platform_name", nullable = false, unique = true, length = 50)
    private String platformName;

    @Builder.Default
    private boolean enabled = true;

    @Column(name = "daily_limit")
    @Builder.Default
    private int dailyLimit = 15;

    @Column(name = "min_delay_seconds")
    @Builder.Default
    private int minDelaySeconds = 300;

    @Column(name = "current_count_today")
    @Builder.Default
    private int currentCountToday = 0;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    @Column(name = "is_paused")
    @Builder.Default
    private boolean paused = false;

    // NOTE (JobPilot v2.0, spec §1/§26): credential & session-cookie storage removed.
    // No LinkedIn/Naukri email, password, or session cookies are ever persisted.
    // This entity is now neutral per-source enablement / rate metadata (§57 Source Health).

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (lastResetDate == null) lastResetDate = LocalDate.now();
    }
}

