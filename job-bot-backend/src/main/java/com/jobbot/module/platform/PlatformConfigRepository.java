package com.jobbot.module.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, UUID> {

    Optional<PlatformConfig> findByPlatformNameIgnoreCase(String platformName);

    @Modifying
    @Query("update PlatformConfig p set p.currentCountToday = p.currentCountToday + 1 " +
            "where p.id = :id")
    int incrementCount(@Param("id") UUID id);
}

