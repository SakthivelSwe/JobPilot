package com.jobbot.module.account;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.candidate.CandidateProfileRepository;
import com.jobbot.module.candidate.ResumeSourceDocumentRepository;
import com.jobbot.module.criteria.CriteriaRepository;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.ApplicationCapability;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.manualqueue.ManualQueueRepository;
import com.jobbot.module.role.TargetRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock JobPostingRepository postingRepository;
    @Mock CandidateProfileRepository profileRepository;
    @Mock CriteriaRepository criteriaRepository;
    @Mock TargetRoleRepository targetRoleRepository;
    @Mock ManualQueueRepository manualQueueRepository;
    @Mock ResumeSourceDocumentRepository resumeDocRepository;

    @InjectMocks ExportService service;

    @Test
    void applicationsCsvEscapesCommasAndQuotes() {
        when(applicationRepository.findAll()).thenReturn(List.of(
                Application.builder()
                        .company("Acme, Inc").title("Java \"Senior\" Dev")
                        .platform("GREENHOUSE").status("applied")
                        .atsScore(BigDecimal.valueOf(91)).autoApplied(false).build()));

        String csv = service.applicationsCsv();
        String[] lines = csv.strip().split("\n");
        assertEquals("id,company,title,platform,status,atsScore,autoApplied,appliedAt", lines[0]);
        assertTrue(lines[1].contains("\"Acme, Inc\""), "comma field must be quoted");
        assertTrue(lines[1].contains("\"Java \"\"Senior\"\" Dev\""), "quotes must be doubled");
        assertTrue(lines[1].contains("false"));
    }

    @Test
    void jobsCsvHasHeaderAndRow() {
        when(postingRepository.findAll()).thenReturn(List.of(
                JobPosting.builder()
                        .source(AtsType.GREENHOUSE).externalId("x1").title("Backend Engineer")
                        .company("Ramp").location("Remote").remoteType("REMOTE")
                        .applicationCapability(ApplicationCapability.ASSISTED_APPLY)
                        .matchScore(88).recommendation("APPLY").sourceUrl("https://x/job").build()));

        String csv = service.jobsCsv();
        String[] lines = csv.strip().split("\n");
        assertTrue(lines[0].startsWith("id,source,title,company,location"));
        assertTrue(lines[1].contains("Backend Engineer"));
        assertTrue(lines[1].contains("ASSISTED_APPLY"));
        assertTrue(lines[1].contains("88"));
    }
}

