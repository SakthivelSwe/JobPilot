package com.jobbot.module.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    JobPostingRepository repository;

    @InjectMocks
    DeduplicationService service;

    private JobPosting candidate() {
        return JobPosting.builder()
                .source(AtsType.GREENHOUSE).externalId("ext-1")
                .title("Java Engineer").company("Acme").location("Bengaluru")
                .normalizedHash("hash-abc")
                .build();
    }

    @Test
    void newWhenNotSeenBefore() {
        when(repository.findBySourceAndExternalId(AtsType.GREENHOUSE, "ext-1"))
                .thenReturn(Optional.empty());
        when(repository.findByNormalizedHash("hash-abc")).thenReturn(List.of());

        var result = service.check(candidate());
        assertEquals(DeduplicationService.Outcome.NEW, result.outcome());
        assertNull(result.existing());
    }

    @Test
    void alreadySeenWhenSameSourceExternalId() {
        JobPosting existing = candidate();
        when(repository.findBySourceAndExternalId(AtsType.GREENHOUSE, "ext-1"))
                .thenReturn(Optional.of(existing));

        var result = service.check(candidate());
        assertEquals(DeduplicationService.Outcome.ALREADY_SEEN, result.outcome());
        assertSame(existing, result.existing());
    }

    @Test
    void crossSourceDuplicateWhenHashMatchesDifferentSource() {
        JobPosting existing = JobPosting.builder()
                .source(AtsType.ASHBY).externalId("other").normalizedHash("hash-abc").build();
        when(repository.findBySourceAndExternalId(AtsType.GREENHOUSE, "ext-1"))
                .thenReturn(Optional.empty());
        when(repository.findByNormalizedHash("hash-abc")).thenReturn(List.of(existing));

        var result = service.check(candidate());
        assertEquals(DeduplicationService.Outcome.CROSS_SOURCE_DUPLICATE, result.outcome());
        assertSame(existing, result.existing());
    }

    @Test
    void mergeSourceHistoryAppendsToken() {
        JobPosting existing = JobPosting.builder()
                .source(AtsType.ASHBY).externalId("other").normalizedHash("hash-abc").build();
        when(repository.save(existing)).thenReturn(existing);

        service.mergeSourceHistory(existing, candidate());
        assertTrue(existing.getSourcesSeen().contains("GREENHOUSE:ext-1"));
        verify(repository).save(existing);
    }
}

