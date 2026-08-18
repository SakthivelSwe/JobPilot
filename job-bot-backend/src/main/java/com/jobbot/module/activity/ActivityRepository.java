package com.jobbot.module.activity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<ActivityEvent, UUID> {
    List<ActivityEvent> findByOrderByCreatedAtDesc(Pageable pageable);
}

