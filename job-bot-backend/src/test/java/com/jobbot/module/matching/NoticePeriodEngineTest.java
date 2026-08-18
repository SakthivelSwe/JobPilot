package com.jobbot.module.matching;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoticePeriodEngineTest {

    private final NoticePeriodEngine engine = new NoticePeriodEngine();
    private final LocalDate today = LocalDate.of(2026, 8, 17);

    @Test
    void parsesJobRequirements() {
        assertEquals(0, engine.parseRequiredMaxDays("Immediate"));
        assertEquals(15, engine.parseRequiredMaxDays("15 days"));
        assertEquals(30, engine.parseRequiredMaxDays("1 month"));
        assertEquals(60, engine.parseRequiredMaxDays("2 months"));
        assertEquals(null, engine.parseRequiredMaxDays("Any"));
        assertEquals(null, engine.parseRequiredMaxDays("Unknown"));
    }

    @Test
    void lastWorkingDateTakesPrecedenceOverNoticeDays() {
        // LWD 20 days away should override a stored 90-day notice.
        Integer avail = engine.candidateAvailableInDays(90, today.plusDays(20), today);
        assertEquals(20, avail);
    }

    @Test
    void fallsBackToNoticeDaysWhenNoLwd() {
        assertEquals(60, engine.candidateAvailableInDays(60, null, today));
    }

    @Test
    void compatibleWhenWithinWindow() {
        assertEquals(NoticeCompatibility.COMPATIBLE, engine.classify(30, 45));
        assertEquals(NoticeCompatibility.COMPATIBLE, engine.classify(30, 30));
    }

    @Test
    void recruiterApprovalWhenSlightlyOver() {
        // 60-day candidate vs 30-day job → 30 over → within slack.
        assertEquals(NoticeCompatibility.RECRUITER_APPROVAL, engine.classify(60, 30));
    }

    @Test
    void majorMismatchWhenWellOver() {
        // 90-day candidate vs 15-day job → 75 over → major mismatch.
        assertEquals(NoticeCompatibility.MAJOR_MISMATCH, engine.classify(90, 15));
    }

    @Test
    void unknownWhenJobConstraintButCandidateUnknown() {
        assertEquals(NoticeCompatibility.UNKNOWN, engine.classify(null, 30));
    }

    @Test
    void anyRequirementIsAlwaysCompatible() {
        assertEquals(NoticeCompatibility.COMPATIBLE, engine.classify(90, null));
    }

    @Test
    void endToEndFromRawInputs() {
        assertEquals(NoticeCompatibility.COMPATIBLE,
                engine.classify(60, null, "90 days", today));
        assertEquals(NoticeCompatibility.MAJOR_MISMATCH,
                engine.classify(null, today.plusDays(90), "15 days", today));
    }
}

