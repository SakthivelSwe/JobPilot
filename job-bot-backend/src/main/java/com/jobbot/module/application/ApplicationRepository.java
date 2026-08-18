package com.jobbot.module.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByCompanyIgnoreCase(String company);

    Page<Application> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}

