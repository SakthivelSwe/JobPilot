package com.jobbot.module.discovery.adapter;

import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.role.TargetRole;

import java.util.List;

/**
 * A search-based discovery adapter (Naukri / LinkedIn / Indeed). Unlike
 * {@link JobSourceAdapter}, which fetches all postings for a given ATS-configured
 * {@link com.jobbot.module.company.Company}, this adapter runs a keyword/location
 * search derived from a {@link TargetRole} + {@link JobCriteria}.
 */
public interface SearchBasedAdapter {

    AtsType type();

    /**
     * Run the search and return raw postings. Implementations must:
     *  - use conservative page limits (2–3 pages max)
     *  - sleep between page requests (1.5–3.5 s randomised)
     *  - use a stable, honest User-Agent
     *  - never authenticate; only public/guest endpoints
     */
    List<DiscoveredPosting> discover(TargetRole role, JobCriteria criteria);
}

