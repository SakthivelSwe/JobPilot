package com.jobbot.module.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, UUID> {

    /** Tenant-scoped lookup — matches platform name for the current user only. */
    @Query("SELECT p FROM PlatformConfig p WHERE p.userId = :userId AND upper(p.platformName) = upper(:platformName)")
    Optional<PlatformConfig> findByUserIdAndPlatformNameIgnoreCase(
            @Param("userId") String userId,
            @Param("platformName") String platformName);

    /** Legacy (non-tenant) — kept for seeding logic that runs within a seeded security context. */
    Optional<PlatformConfig> findByPlatformNameIgnoreCase(String platformName);

    /** Explicit JPQL to ensure tenant (user_id) filtering is applied on list. */
    @Query("SELECT p FROM PlatformConfig p WHERE p.userId = :userId")
    List<PlatformConfig> findAllForCurrentTenant(@Param("userId") String userId);

    @Modifying
    @Query("update PlatformConfig p set p.currentCountToday = p.currentCountToday + 1 " +
            "where p.id = :id")
    int incrementCount(@Param("id") UUID id);
}

