package com.jobbot.module.criteria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CriteriaRepository extends JpaRepository<JobCriteria, UUID> {
    List<JobCriteria> findAllByActiveTrue();
}

