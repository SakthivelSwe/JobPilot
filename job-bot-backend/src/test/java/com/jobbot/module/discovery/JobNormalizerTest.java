package com.jobbot.module.discovery;

import com.jobbot.module.discovery.adapter.DiscoveredPosting;
import com.jobbot.module.matching.LocationEngine;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JobNormalizerTest {

    private final JobNormalizer normalizer = new JobNormalizer(new LocationEngine());

    private DiscoveredPosting posting(String location, String remoteHint, String empType, String desc) {
        return new DiscoveredPosting(AtsType.GREENHOUSE, "ext-1", "Senior Java Engineer",
                "Acme", location, "https://x/job", "https://x/job", desc, empType, remoteHint,
                LocalDate.of(2026, 8, 1));
    }

    @Test
    void normalizesCityAndWorkMode() {
        JobPosting jp = normalizer.normalize(posting("Bangalore", "Bangalore", "FullTime",
                "Java Spring Boot 2-4 years"));
        assertEquals("Bengaluru", jp.getLocation());
        assertEquals("FULL_TIME", jp.getEmploymentType());
    }

    @Test
    void remoteHintDrivesRemoteType() {
        JobPosting jp = normalizer.normalize(posting("Remote - India", "remote", "FullTime", "Java"));
        assertEquals("REMOTE", jp.getRemoteType());
    }

    @Test
    void extractsExperienceRange() {
        JobPosting jp = normalizer.normalize(posting("Chennai", "Chennai", "FullTime",
                "We need 2-4 years of Java experience"));
        assertEquals(2, jp.getMinimumExperience());
        assertEquals(4, jp.getMaximumExperience());
    }

    @Test
    void ambiguousExperienceLeftNull() {
        JobPosting jp = normalizer.normalize(posting("Chennai", "Chennai", "FullTime",
                "Great Java role for the right person"));
        assertNull(jp.getMinimumExperience());
        assertNull(jp.getMaximumExperience());
    }

    @Test
    void hashIsStableAcrossSpellingsOfCity() {
        // Bangalore vs Bengaluru should collapse to the same dedup hash.
        String h1 = normalizer.computeHash("Acme", "Senior Java Engineer", "Bengaluru");
        JobPosting jp = normalizer.normalize(posting("Bangalore", "Bangalore", "FullTime", "Java"));
        assertEquals(h1, jp.getNormalizedHash());
    }
}

