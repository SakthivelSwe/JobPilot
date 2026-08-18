package com.jobbot.module.candidate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "achievement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    @JsonIgnore
    private CandidateProfile profile;

    @Column(columnDefinition = "TEXT")
    private String description;
}


