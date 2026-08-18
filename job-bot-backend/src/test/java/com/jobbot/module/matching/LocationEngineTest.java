package com.jobbot.module.matching;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationEngineTest {

    private final LocationEngine engine = new LocationEngine();

    @Test
    void normalizesCityAliases() {
        assertEquals("Bengaluru", engine.normalizeCity("Bangalore"));
        assertEquals("Bengaluru", engine.normalizeCity("bengaluru"));
        assertEquals("Gurugram", engine.normalizeCity("Gurgaon"));
        assertEquals("Chennai", engine.normalizeCity("madras"));
    }

    @Test
    void parsesWorkModeFromText() {
        assertEquals(WorkMode.REMOTE, engine.parseWorkMode("Work from anywhere"));
        assertEquals(WorkMode.HYBRID, engine.parseWorkMode("3 days office"));
        assertEquals(WorkMode.HYBRID, engine.parseWorkMode("Hybrid"));
        assertEquals(WorkMode.ONSITE, engine.parseWorkMode("Work from office"));
    }

    @Test
    void ambiguousWorkModeIsUnknown() {
        // §12: never guess when ambiguous.
        assertEquals(WorkMode.UNKNOWN, engine.parseWorkMode("competitive salary"));
        assertEquals(WorkMode.UNKNOWN, engine.parseWorkMode(null));
    }

    @Test
    void remoteJobMatchesWhenCandidateAcceptsRemote() {
        assertEquals(LocationMatch.REMOTE,
                engine.match(List.of("Chennai"), true, false, null, WorkMode.REMOTE));
        assertEquals(LocationMatch.NONE,
                engine.match(List.of("Chennai"), false, false, null, WorkMode.REMOTE));
    }

    @Test
    void preferredCityMatchesRegardlessOfSpelling() {
        assertEquals(LocationMatch.CITY,
                engine.match(List.of("Bangalore"), false, false, "Bengaluru", WorkMode.ONSITE));
    }

    @Test
    void relocationWhenCityNotPreferredButOpen() {
        assertEquals(LocationMatch.RELOCATION,
                engine.match(List.of("Chennai"), false, true, "Pune", WorkMode.ONSITE));
        assertEquals(LocationMatch.NONE,
                engine.match(List.of("Chennai"), false, false, "Pune", WorkMode.ONSITE));
    }

    @Test
    void unknownWhenNoCityAndNotRemote() {
        assertEquals(LocationMatch.UNKNOWN,
                engine.match(List.of("Chennai"), true, true, null, WorkMode.ONSITE));
    }
}

