package com.jobbot.module.candidate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * A normalized skill on the candidate profile, with proficiency and evidence (spec §6).
 * Proficiency is never auto-upgraded to EXPERT by the parser.
 */
@Entity
@Table(name = "candidate_skill")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkill {
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

    /** Canonical/normalized skill name, e.g. "Spring Boot", "Kafka", "AWS". */
    @Column(nullable = false)
    private String name;

    /** Optional category, e.g. LANGUAGE, FRAMEWORK, CLOUD, MESSAGING, DATABASE, TOOL. */
    private String category;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Proficiency proficiency = Proficiency.UNKNOWN;

    /** true once the user confirms this skill during profile verification. */
    @Column(name = "user_verified")
    @Builder.Default
    private boolean userVerified = false;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SkillEvidence> evidence = new ArrayList<>();

    public void addEvidence(SkillEvidence e) { e.setSkill(this); evidence.add(e); }
}




