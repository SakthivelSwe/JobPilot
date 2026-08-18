package com.jobbot.module.company;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.company.dto.CompanyDTO;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.SourceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Company registry management (spec §10).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository repository;

    public List<Company> getAll() {
        return repository.findAll();
    }

    public List<Company> getActive() {
        return repository.findAllByActiveTrue();
    }

    public Company getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Company not found: " + id));
    }

    @Transactional
    public Company create(CompanyDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new JobBotException("Company name is required");
        }
        AtsType ats = parseAts(dto.getAtsType());
        Company company = Company.builder()
                .name(dto.getName())
                .domain(dto.getDomain())
                .careersUrl(dto.getCareersUrl())
                .country(dto.getCountry())
                .industry(dto.getIndustry())
                .companyType(dto.getCompanyType())
                .atsType(ats)
                .atsToken(dto.getAtsToken())
                .sourceStatus(isManual(ats) ? SourceStatus.MANUAL : SourceStatus.HEALTHY)
                .active(dto.getActive() == null || dto.getActive())
                .build();
        return repository.save(company);
    }

    @Transactional
    public Company update(UUID id, CompanyDTO dto) {
        Company c = getById(id);
        if (dto.getName() != null) c.setName(dto.getName());
        if (dto.getDomain() != null) c.setDomain(dto.getDomain());
        if (dto.getCareersUrl() != null) c.setCareersUrl(dto.getCareersUrl());
        if (dto.getCountry() != null) c.setCountry(dto.getCountry());
        if (dto.getIndustry() != null) c.setIndustry(dto.getIndustry());
        if (dto.getCompanyType() != null) c.setCompanyType(dto.getCompanyType());
        if (dto.getAtsType() != null) c.setAtsType(parseAts(dto.getAtsType()));
        if (dto.getAtsToken() != null) c.setAtsToken(dto.getAtsToken());
        if (dto.getActive() != null) c.setActive(dto.getActive());
        return repository.save(c);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new JobBotException("Company not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void markChecked(Company company, SourceStatus status) {
        company.setSourceStatus(status);
        company.setLastChecked(OffsetDateTime.now());
        repository.save(company);
    }

    /** Idempotent seed helper (used by the seeder). */
    @Transactional
    public Company seed(String name, AtsType ats, String token, String country, String type) {
        return repository.findByAtsTypeAndAtsToken(ats, token).orElseGet(() ->
                repository.save(Company.builder()
                        .name(name).atsType(ats).atsToken(token)
                        .country(country).companyType(type)
                        .sourceStatus(isManual(ats) ? SourceStatus.MANUAL : SourceStatus.HEALTHY)
                        .active(true).build()));
    }

    private static boolean isManual(AtsType ats) {
        return ats == AtsType.MANUAL || ats == AtsType.LINKEDIN
                || ats == AtsType.NAUKRI || ats == AtsType.OTHER;
    }

    private static AtsType parseAts(String raw) {
        if (raw == null || raw.isBlank()) return AtsType.MANUAL;
        try {
            return AtsType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JobBotException("Unknown atsType: " + raw);
        }
    }
}

