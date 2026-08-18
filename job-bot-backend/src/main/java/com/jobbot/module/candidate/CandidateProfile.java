package com.jobbot.module.candidate;

import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The master candidate profile — the primary source of truth (spec §3).
 * Child collections (skills, experiences, projects, education, etc.) hang off this.
 *
 * A profile is only considered authoritative once {@code verified == true}, which
 * happens when the user confirms an extraction (spec §4 — never silently overwrite).
 */
@Entity
@Table(name = "candidate_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "user_id")
    private String userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String email;
    private String phone;

    @Column(name = "current_location")
    private String currentLocation;

    @Convert(converter = StringListConverter.class)
    @Column(name = "preferred_locations", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> preferredLocations = new ArrayList<>();

    /** REMOTE / HYBRID / ONSITE tokens the candidate accepts. */
    @Convert(converter = StringListConverter.class)
    @Column(name = "preferred_work_modes", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> preferredWorkModes = new ArrayList<>();

    @Column(name = "years_of_experience", precision = 4, scale = 1)
    private BigDecimal yearsOfExperience;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "last_working_date")
    private LocalDate lastWorkingDate;

    @Column(name = "expected_salary")
    private BigDecimal expectedSalary;

    @Column(name = "minimum_salary")
    private BigDecimal minimumSalary;

    @Column(name = "relocation_preference")
    private String relocationPreference;

    @Column(name = "remote_preference")
    private String remotePreference;

    @Column(name = "work_authorization")
    private String workAuthorization;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Convert(converter = StringListConverter.class)
    @Column(name = "target_roles", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> targetRoles = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "excluded_roles", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> excludedRoles = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "preferred_companies", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> preferredCompanies = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "excluded_companies", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> excludedCompanies = new ArrayList<>();

    /** true once the user has verified the extracted data (spec §4). */
    @Column(name = "verified")
    @Builder.Default
    private boolean verified = false;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Education> education = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Certification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Achievement> achievements = new ArrayList<>();

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

    /** Wire a child to this parent (keeps both sides of the relationship consistent). */
    public void addSkill(CandidateSkill s) { s.setProfile(this); skills.add(s); }
    public void addExperience(WorkExperience e) { e.setProfile(this); experiences.add(e); }
    public void addProject(Project p) { p.setProfile(this); projects.add(p); }
    public void addEducation(Education e) { e.setProfile(this); education.add(e); }
    public void addCertification(Certification c) { c.setProfile(this); certifications.add(c); }
    public void addAchievement(Achievement a) { a.setProfile(this); achievements.add(a); }
}

