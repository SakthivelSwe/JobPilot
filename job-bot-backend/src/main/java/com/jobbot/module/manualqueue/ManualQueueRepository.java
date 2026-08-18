package com.jobbot.module.manualqueue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualQueueRepository extends JpaRepository<ManualQueueEntry, UUID> {

    Optional<ManualQueueEntry> findByPostingId(UUID postingId);

    List<ManualQueueEntry> findByStatusOrderByMatchScoreDesc(ManualQueueStatus status);

    List<ManualQueueEntry> findAllByOrderByMatchScoreDesc();

    long countByStatus(ManualQueueStatus status);
}

