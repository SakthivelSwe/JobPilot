package com.jobbot.module.candidate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobbot.common.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
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

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> technologies = new ArrayList<>();
}
