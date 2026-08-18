package com.jobbot.module.company;

import com.jobbot.module.discovery.AtsType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findAllByActiveTrue();

    List<Company> findAllByActiveTrueAndAtsTypeIn(List<AtsType> atsTypes);

    Optional<Company> findByAtsTypeAndAtsToken(AtsType atsType, String atsToken);

    long countByActiveTrue();
}

