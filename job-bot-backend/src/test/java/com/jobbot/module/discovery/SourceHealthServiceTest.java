package com.jobbot.module.discovery;

import com.jobbot.module.company.Company;
import com.jobbot.module.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceHealthServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock JobPostingRepository postingRepository;

    @InjectMocks SourceHealthService service;

    private Company gh() {
        return Company.builder().name("Acme").atsType(AtsType.GREENHOUSE)
                .atsToken("acme").sourceStatus(SourceStatus.HEALTHY).active(true).build();
    }

    @Test
    void indiaPlatformsAreActive() {
        when(companyRepository.findAllByActiveTrue()).thenReturn(List.of());

        Map<String, SourceStatus> byName = service.health().stream()
                .collect(Collectors.toMap(SourceHealthService.SourceHealthRow::source,
                        SourceHealthService.SourceHealthRow::status));

        // Naukri / LinkedIn / Indeed: primary India discovery, always active.
        assertEquals(SourceStatus.HEALTHY, byName.get("NAUKRI"));
        assertEquals(SourceStatus.HEALTHY, byName.get("LINKEDIN"));
        assertEquals(SourceStatus.HEALTHY, byName.get("INDEED"));
    }

    @Test
    void greenhouseHealthySurfacesWhenSeeded() {
        when(companyRepository.findAllByActiveTrue()).thenReturn(List.of(gh()));

        Map<String, SourceStatus> byName = service.health().stream()
                .collect(Collectors.toMap(SourceHealthService.SourceHealthRow::source,
                        SourceHealthService.SourceHealthRow::status));

        assertEquals(SourceStatus.HEALTHY, byName.get("GREENHOUSE"));
    }

    @Test
    void coverageCountsActiveSources() {
        when(companyRepository.findAllByActiveTrue()).thenReturn(List.of(gh()));
        lenient().when(companyRepository.countByActiveTrue()).thenReturn(1L);
        lenient().when(postingRepository.count()).thenReturn(42L);

        SourceHealthService.CoverageStats c = service.coverage();
        assertEquals(1, c.companiesMonitored());
        assertEquals(42, c.postingsTotal());
        // Naukri + LinkedIn + Indeed + Greenhouse = at least 4 active sources.
        assertTrue(c.sourcesActive() >= 4);
    }
}
