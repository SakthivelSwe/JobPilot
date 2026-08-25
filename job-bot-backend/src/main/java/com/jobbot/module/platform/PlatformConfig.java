package com.jobbot.module.platform;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_config")
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
    @Column(name = "platform_name", nullable = false, length = 50)
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
    // Session linking (v2.1): sessionFilePath stores a LOCAL path to an encrypted
    // storageState file only. Raw cookie values are never persisted in the database.

    /**
     * Human-readable session status: DISCONNECTED | CONNECTED | EXPIRED | ERROR.
     * Defaults to DISCONNECTED (no account linked).
     */
    @Column(name = "session_status", length = 20)
    @Builder.Default
    private String sessionStatus = "DISCONNECTED";

    /**
     * Whether the stored session file is currently valid (last confirmed reachable).
     * Checked lazily on every apply attempt and eagerly on /validate.
     */
    @Column(name = "session_active")
    @Builder.Default
    private Boolean sessionActive = false;

    /**
     * Display-only: the username/email observed after login. Never used for auth.
     * Shown in the Settings UI so the user can confirm which account is linked.
     */
    @Column(name = "session_username", length = 150)
    private String sessionUsername;

    /** When the session was last successfully connected / re-validated. */
    @Column(name = "session_connected_at")
    private OffsetDateTime sessionConnectedAt;

    /**
     * Absolute path to the encrypted Playwright storageState JSON file on the
     * local machine. The file itself is AES-256 encrypted.
     * This value is NEVER sent to the frontend.
     */
    @Column(name = "session_file_path", length = 512)
    private String sessionFilePath;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (lastResetDate == null) lastResetDate = LocalDate.now();
    }
}

