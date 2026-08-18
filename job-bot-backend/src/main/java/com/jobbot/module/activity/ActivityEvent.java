package com.jobbot.module.activity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A persisted, truthful record of something that actually happened (rule 56/72).
 * Events are ONLY created from real system actions — never fabricated to fill the UI.
 */
@Entity
@Table(name = "activity_event", indexes = @Index(name = "idx_activity_created", columnList = "created_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEvent {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** DISCOVERY / APPLICATION / INTERVIEW / PROFILE / CRITERIA / QUEUE ... */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 500)
    private String detail;

    /** Optional link back to the entity this event is about. */
    @Column(name = "entity_type", length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

