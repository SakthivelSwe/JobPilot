package com.jobbot.module.analytics;

import com.jobbot.module.activity.ActivityRepository;
import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentumServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock ActivityRepository activityRepository;
    @InjectMocks MomentumService service;

    private Application app(String status, OffsetDateTime appliedAt, OffsetDateTime interviewDate) {
        return Application.builder()
                .status(status).appliedAt(appliedAt).interviewDate(interviewDate)
                .lastUpdated(appliedAt).build();
    }

    @Test
    void notAvailableWhenNoActivity() {
        when(applicationRepository.findAll()).thenReturn(List.of());
        lenient().when(activityRepository.findByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        MomentumService.Momentum m = service.compute();
        assertFalse(m.available());
        assertNull(m.score());
        assertTrue(m.message().toLowerCase().contains("not enough"));
    }

    @Test
    void scoreIsDeterministicAndCapped() {
        OffsetDateTime now = OffsetDateTime.now();
        // 3 applications this week, 1 interview → pApps=18, pInts=15
        when(applicationRepository.findAll()).thenReturn(List.of(
                app("applied", now.minusDays(1), null),
                app("applied", now.minusDays(2), null),
                app("interview", now.minusDays(1), now.minusDays(1))));
        when(activityRepository.findByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        MomentumService.Momentum m = service.compute();
        assertTrue(m.available());
        // interview app counts as application(3×6 capped 5→18) + interview(1×15) + response(interview status →5)
        assertNotNull(m.score());
        assertTrue(m.score() > 0 && m.score() <= 100);
        assertEquals(4, m.factors().size());
        // Idempotent: same input → same score
        assertEquals(m.score(), service.compute().score());
    }

    @Test
    void labelReflectsScoreBand() {
        OffsetDateTime now = OffsetDateTime.now();
        when(applicationRepository.findAll()).thenReturn(List.of(app("applied", now.minusDays(1), null)));
        when(activityRepository.findByOrderByCreatedAtDesc(any())).thenReturn(List.of());
        MomentumService.Momentum m = service.compute();
        assertTrue(List.of("Strong week", "Building momentum", "Quiet week").contains(m.label()));
    }
}

