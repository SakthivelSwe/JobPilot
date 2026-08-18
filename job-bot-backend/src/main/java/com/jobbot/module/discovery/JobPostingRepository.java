package com.jobbot.module.discovery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    Optional<JobPosting> findBySourceAndExternalId(AtsType source, String externalId);

    List<JobPosting> findByNormalizedHash(String normalizedHash);

    Page<JobPosting> findByStatus(String status, Pageable pageable);

    long countByCreatedAtAfter(OffsetDateTime since);

    long countByApplicationCapability(ApplicationCapability capability);

    long countByMatchScoreGreaterThanEqual(int score);

    long countByRecommendation(String recommendation);

    long countBySource(AtsType source);

    @Query("select avg(p.matchScore) from JobPosting p where p.matchScore is not null")
    Double averageMatchScore();

    @Query("select coalesce(p.location, 'Unknown') as location, count(p) as cnt "
            + "from JobPosting p group by p.location order by count(p) desc")
    List<Object[]> locationDistribution();
}

