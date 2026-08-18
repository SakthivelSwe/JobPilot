package com.jobbot.module.ats;

import com.jobbot.module.ai.AiProvider;
import com.jobbot.module.ai.usage.AiUsageTracker;
import com.jobbot.module.ats.dto.AtsResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtsServiceTest {

    @Mock AiProvider aiProvider;
    @Mock AiUsageTracker aiUsageTracker;

    @InjectMocks AtsService atsService;

    @Test
    void deterministicScoreWithoutAi() {
        when(aiProvider.isAvailable()).thenReturn(false); // AI off → deterministic only (§18/§54)

        AtsResult r = atsService.analyze(
                "Java Spring Boot developer with REST experience",
                "We need Java and Spring Boot engineers building REST services",
                List.of("Java", "Spring Boot", "Kafka"),
                null, 0, 0);

        assertTrue(r.getMatchedKeywords().contains("Java"));
        assertTrue(r.getMatchedKeywords().contains("Spring Boot"));
        assertTrue(r.getMissingKeywords().contains("Kafka"));
        assertTrue(r.getScore() > 0 && r.getScore() <= 100);
        assertNull(r.getAiNote()); // no AI note when provider unavailable
    }

    @Test
    void fullSkillOverlapScoresHigherThanPartial() {
        when(aiProvider.isAvailable()).thenReturn(false);

        AtsResult full = atsService.analyzeJobFit(
                "Java Spring Boot Kafka",
                "Java Spring Boot Kafka microservices",
                List.of("Java", "Spring Boot", "Kafka"));
        AtsResult partial = atsService.analyzeJobFit(
                "Java only",
                "Java Spring Boot Kafka microservices",
                List.of("Java", "Spring Boot", "Kafka"));

        assertTrue(full.getScore() > partial.getScore());
    }

    @Test
    void missingJobDescriptionFailsGracefully() {
        AtsResult r = atsService.analyze("resume", "", List.of("Java"), null, 0, 0);
        assertFalse(r.isShouldApply());
    }
}

