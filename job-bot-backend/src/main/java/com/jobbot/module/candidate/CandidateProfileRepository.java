package com.jobbot.module.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    List<CandidateProfile> findByVerifiedTrue();

    /** Tenant-scoped query: only returns profiles belonging to the given user. */
    @Query("SELECT p FROM CandidateProfile p WHERE p.userId = :userId ORDER BY p.updatedAt DESC")
    List<CandidateProfile> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT p FROM CandidateProfile p WHERE p.userId = :userId ORDER BY p.updatedAt DESC LIMIT 1")
    Optional<CandidateProfile> findLatestByUserId(@Param("userId") String userId);
}

