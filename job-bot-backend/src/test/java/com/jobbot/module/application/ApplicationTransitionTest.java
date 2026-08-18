package com.jobbot.module.application;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.activity.ActivityService;
import com.jobbot.module.job.JobService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationTransitionTest {

    private final ApplicationService service = new ApplicationService(
            mock(ApplicationRepository.class), mock(JobService.class), mock(ActivityService.class));

    @Test
    void forwardTransitionsAllowed() {
        assertDoesNotThrow(() -> service.validateTransition("applied", "interview"));
        assertDoesNotThrow(() -> service.validateTransition("interview", "offer"));
        assertDoesNotThrow(() -> service.validateTransition("applied", "offer")); // skip ahead ok
    }

    @Test
    void sameStatusIsNoOp() {
        assertDoesNotThrow(() -> service.validateTransition("interview", "interview"));
    }

    @Test
    void anyStageCanReachTerminal() {
        assertDoesNotThrow(() -> service.validateTransition("applied", "rejected"));
        assertDoesNotThrow(() -> service.validateTransition("offer", "withdrawn"));
    }

    @Test
    void terminalCanReopenForCorrection() {
        assertDoesNotThrow(() -> service.validateTransition("rejected", "interview"));
    }

    @Test
    void backwardActiveTransitionRejected() {
        // The exact case the spec calls out: cannot move an offer back into the pipeline.
        JobBotException ex = assertThrows(JobBotException.class,
                () -> service.validateTransition("offer", "applied"));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot move back"));
        assertThrows(JobBotException.class,
                () -> service.validateTransition("interview", "viewed"));
    }

    @Test
    void unknownStatusesDoNotBlock() {
        assertDoesNotThrow(() -> service.validateTransition("applied", "something_custom"));
        assertDoesNotThrow(() -> service.validateTransition(null, "applied"));
    }
}

