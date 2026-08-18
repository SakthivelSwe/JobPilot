package com.jobbot.module.ai.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    Optional<AiUsage> findByUsageDateAndProviderAndFeature(LocalDate date, String provider, String feature);

    List<AiUsage> findByUsageDate(LocalDate date);

    @Query("select coalesce(sum(u.requests), 0) from AiUsage u where u.usageDate = :date")
    long totalRequestsOn(@Param("date") LocalDate date);
}

