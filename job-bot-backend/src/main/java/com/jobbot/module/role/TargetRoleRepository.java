package com.jobbot.module.role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TargetRoleRepository extends JpaRepository<TargetRole, UUID> {

    List<TargetRole> findAllByOrderByPriorityAsc();

    List<TargetRole> findAllByActiveTrueOrderByPriorityAsc();
}

