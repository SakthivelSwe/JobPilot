package com.jobbot.module.queue;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobQueueRepository extends JpaRepository<JobQueueEntry, UUID> {

    Optional<JobQueueEntry> findByExternalIdAndPlatform(String externalId, String platform);

    Page<JobQueueEntry> findByStatusOrderByMatchScoreDescCreatedAtDesc(
            JobQueueStatus status, Pageable pageable);

    Page<JobQueueEntry> findByStatusInOrderByMatchScoreDescCreatedAtDesc(
            List<JobQueueStatus> statuses, Pageable pageable);

    long countByStatus(JobQueueStatus status);

    /**
     * Atomically pick the next APPROVED job for a platform. The caller must
     * transition it to AUTO_APPLYING before returning to the engine to prevent
     * double-pick.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from JobQueueEntry q "
            + "where q.status = com.jobbot.module.queue.JobQueueStatus.APPROVED "
            + "and q.platform = :platform "
            + "order by q.matchScore desc, q.createdAt asc")
    List<JobQueueEntry> findApprovedForPlatform(@Param("platform") String platform, Pageable p);

    @Query("select q from JobQueueEntry q "
            + "where q.status = com.jobbot.module.queue.JobQueueStatus.PENDING_REVIEW "
            + "and q.matchScore >= :threshold")
    List<JobQueueEntry> findPendingAboveThreshold(@Param("threshold") BigDecimal threshold);
}

