package com.jobbot.module.candidate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A single piece of evidence backing a skill (spec §6): a project, a work item,
 * or a note like "Current learning". Keeps skills honest and traceable (spec §21).
 */
@Entity
@Table(name = "skill_evidence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    @JsonIgnore
    private CandidateSkill skill;

    /** e.g. PROJECT, WORK, CERTIFICATION, LEARNING, RESUME_MENTION. */
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;
}


