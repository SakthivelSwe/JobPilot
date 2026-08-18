package com.jobbot.module.ai.usage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiUsageTrackerTest {

    @Mock
    AiUsageRepository repository;

    @InjectMocks
    AiUsageTracker tracker;

    @Test
    void canCallRespectsDailyLimit() {
        ReflectionTestUtils.setField(tracker, "dailyLimit", 2);
        when(repository.totalRequestsOn(any())).thenReturn(1L);
        assertTrue(tracker.canCall());
        when(repository.totalRequestsOn(any())).thenReturn(2L);
        assertFalse(tracker.canCall());
    }

    @Test
    void recordIncrementsCounters() {
        when(repository.findByUsageDateAndProviderAndFeature(any(LocalDate.class), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(AiUsage.class))).thenAnswer(inv -> inv.getArgument(0));

        tracker.record("NoOpAiProvider", "ats_enrichment", 100);

        verify(repository).save(argThat(u ->
                u.getRequests() == 1 && u.getEstimatedTokens() == 100
                        && u.getFeature().equals("ats_enrichment")));
    }

    @Test
    void estimateTokensIsCharsOverFour() {
        assertEquals(2, tracker.estimateTokens("abcd", "efgh"));
    }
}

