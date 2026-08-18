package com.jobbot.module.candidate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    List<CandidateProfile> findByVerifiedTrue();
}

