package com.jobbot.module.analytics;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.manualqueue.ManualQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock JobPostingRepository postingRepository;
    @Mock ManualQueueRepository manualQueueRepository;

    @InjectMocks AnalyticsService service;

    @Test
    void learningNotReadyBelowThreshold() {
        when(applicationRepository.findAll()).thenReturn(List.of(
                Application.builder().status("applied").build()));
        Map<String, Object> learning = service.learning();
        assertEquals(false, learning.get("ready"));
        assertTrue(((List<?>) learning.get("recommendations")).isEmpty());
    }

    @Test
    void learningReadyAtThreshold() {
        List<Application> apps = IntStream.range(0, 20)
                .mapToObj(i -> Application.builder()
                        .title("Java Backend Developer").platform("GREENHOUSE")
                        .status(i % 4 == 0 ? "interview" : "applied").build())
                .toList();
        when(applicationRepository.findAll()).thenReturn(apps);
        Map<String, Object> learning = service.learning();
        assertEquals(true, learning.get("ready"));
    }
}

