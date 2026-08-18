package com.jobbot.module.matching;

import com.jobbot.module.discovery.JobPosting;

/** A posting paired with its computed match, for ranked lists (spec §32/§66). */
public record RankedMatch(JobPosting posting, MatchResult match) {}

