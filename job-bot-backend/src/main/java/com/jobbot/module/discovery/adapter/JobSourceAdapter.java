package com.jobbot.module.discovery.adapter;

import com.jobbot.module.company.Company;
import com.jobbot.module.discovery.AtsType;

import java.util.List;

/**
 * A source adapter that fetches public postings for a company (spec §9).
 * Implementations must only use officially permitted, unauthenticated public
 * endpoints — no scraping behind auth, no evasion (spec §1).
 */
public interface JobSourceAdapter {

    AtsType type();

    /** Fetch current public postings for the given company. Network call. */
    List<DiscoveredPosting> fetch(Company company);
}

