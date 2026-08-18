package com.jobbot.module.queue;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.discovery.ApplicationCapability;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.platform.PlatformConfigService;
import com.jobbot.module.resume.variant.ResumeSelectionService;
import com.jobbot.module.resume.variant.ResumeVariant;
import com.jobbot.module.resume.variant.VariantScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobQueueServiceTest {

    @Mock JobQueueRepository repository;
    @Mock PlatformConfigService platformConfigService;
    @Mock ApplicationRepository applicationRepository;
    @Mock ResumeSelectionService resumeSelectionService;

    @InjectMocks JobQueueService service;

    private JobPosting posting;

    @BeforeEach
    void setUp() {
        posting = JobPosting.builder()
                .id(UUID.randomUUID())
                .source(AtsType.NAUKRI)
                .externalId("naukri:12345")
                .title("Java Backend Engineer")
                .company("Acme")
                .location("Chennai")
                .sourceUrl("https://www.naukri.com/jobs/12345")
                .applicationCapability(ApplicationCapability.ASSISTED_APPLY)
                .matchScore(88)
                .recommendation("STRONG_APPLY")
                .build();
    }

    @Test
    void enqueuesHighScoringPostingAsPendingReview() {
        when(repository.findByExternalIdAndPlatform("naukri:12345", "NAUKRI")).thenReturn(Optional.empty());
        when(resumeSelectionService.selectBest(posting))
                .thenReturn(new VariantScore(ResumeVariant.JAVA_BACKEND, "Java Backend", 92, List.of(), true));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JobQueueEntry entry = service.enqueueFromPosting(posting, BigDecimal.valueOf(70));
        assertEquals(JobQueueStatus.PENDING_REVIEW, entry.getStatus());
        assertEquals("NAUKRI", entry.getPlatform());
        assertEquals("JAVA_BACKEND", entry.getResumeVariant());
        assertNull(entry.getFailureReason());
    }

    @Test
    void filtersOutBelowThreshold() {
        posting.setMatchScore(50);
        when(repository.findByExternalIdAndPlatform(anyString(), anyString())).thenReturn(Optional.empty());
        when(resumeSelectionService.selectBest(posting))
                .thenReturn(new VariantScore(ResumeVariant.JAVA_BACKEND, "Java Backend", 60, List.of(), true));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JobQueueEntry entry = service.enqueueFromPosting(posting, BigDecimal.valueOf(70));
        assertEquals(JobQueueStatus.FILTERED_OUT, entry.getStatus());
        assertNotNull(entry.getFailureReason());
    }

    @Test
    void skipRecommendationIsFilteredOut() {
        posting.setRecommendation("SKIP");
        when(repository.findByExternalIdAndPlatform(anyString(), anyString())).thenReturn(Optional.empty());
        when(resumeSelectionService.selectBest(posting))
                .thenReturn(new VariantScore(ResumeVariant.JAVA_BACKEND, "Java Backend", 92, List.of(), true));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JobQueueEntry entry = service.enqueueFromPosting(posting, BigDecimal.valueOf(70));
        assertEquals(JobQueueStatus.FILTERED_OUT, entry.getStatus());
        assertTrue(entry.getFailureReason().toUpperCase().contains("SKIP"));
    }

    @Test
    void deduplicatesByExternalIdAndPlatform() {
        JobQueueEntry existing = JobQueueEntry.builder()
                .externalId("naukri:12345").platform("NAUKRI").status(JobQueueStatus.APPROVED).build();
        when(repository.findByExternalIdAndPlatform("naukri:12345", "NAUKRI"))
                .thenReturn(Optional.of(existing));

        JobQueueEntry entry = service.enqueueFromPosting(posting, BigDecimal.valueOf(70));
        assertSame(existing, entry);
        verify(repository, never()).save(any());
    }

    @Test
    void pickNextRespectsRateLimit() {
        when(platformConfigService.canApply("NAUKRI")).thenReturn(false);
        assertTrue(service.pickNextApproved("NAUKRI").isEmpty());
        verify(repository, never()).findApprovedForPlatform(any(), any());
    }

    @Test
    void pickNextTransitionsToAutoApplying() {
        UUID id = UUID.randomUUID();
        JobQueueEntry approved = JobQueueEntry.builder()
                .id(id).externalId("x").platform("NAUKRI")
                .status(JobQueueStatus.APPROVED)
                .build();
        when(platformConfigService.canApply("NAUKRI")).thenReturn(true);
        when(repository.findApprovedForPlatform(eq("NAUKRI"), any())).thenReturn(List.of(approved));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JobQueueEntry picked = service.pickNextApproved("NAUKRI").orElseThrow();
        assertEquals(JobQueueStatus.AUTO_APPLYING, picked.getStatus());
    }

    @Test
    void markAutoAppliedCreatesApplicationAndIncrementsRate() {
        UUID id = UUID.randomUUID();
        JobQueueEntry entry = JobQueueEntry.builder()
                .id(id).externalId("x").platform("NAUKRI")
                .status(JobQueueStatus.AUTO_APPLYING)
                .company("Acme").title("Java").matchScore(BigDecimal.valueOf(88))
                .build();
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markAutoApplied(id);

        ArgumentCaptor<Application> app = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(app.capture());
        assertTrue(app.getValue().isAutoApplied());
        assertEquals("NAUKRI", app.getValue().getPlatform());
        verify(platformConfigService).incrementCount("NAUKRI");
    }

    @Test
    void markFailedWithCaptchaMovesToManual() {
        UUID id = UUID.randomUUID();
        JobQueueEntry entry = JobQueueEntry.builder()
                .id(id).externalId("x").platform("NAUKRI").status(JobQueueStatus.AUTO_APPLYING).build();
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JobQueueEntry updated = service.markFailed(id, "CAPTCHA detected");
        assertEquals(JobQueueStatus.MANUAL_APPLY, updated.getStatus());
    }

    @Test
    void markFailedGenericGoesToFailedApply() {
        UUID id = UUID.randomUUID();
        JobQueueEntry entry = JobQueueEntry.builder()
                .id(id).externalId("x").platform("NAUKRI").status(JobQueueStatus.AUTO_APPLYING).build();
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        when(repository.save(any(JobQueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JobQueueEntry updated = service.markFailed(id, "form submit timeout");
        assertEquals(JobQueueStatus.FAILED_APPLY, updated.getStatus());
        assertTrue(updated.getFailureReason().contains("timeout"));
    }
}

