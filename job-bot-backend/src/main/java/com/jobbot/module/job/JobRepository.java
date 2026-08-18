package com.jobbot.module.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByPlatformAndPlatformJobId(String platform, String platformJobId);

    Page<Job> findByStatus(String status, Pageable pageable);

    Page<Job> findByPlatform(String platform, Pageable pageable);
}

