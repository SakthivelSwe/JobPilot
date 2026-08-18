package com.jobbot.module.discovery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Deduplicates postings (spec §13) using, in order:
 * (1) source + external id, then (2) the normalized company|title|location hash.
 * Keeps a source-history so the same job seen on multiple sources is shown once.
 */
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final JobPostingRepository repository;

    public enum Outcome { NEW, ALREADY_SEEN, CROSS_SOURCE_DUPLICATE }

    public record DedupResult(Outcome outcome, JobPosting existing) {}

    public DedupResult check(JobPosting candidate) {
        Optional<JobPosting> bySource =
                repository.findBySourceAndExternalId(candidate.getSource(), candidate.getExternalId());
        if (bySource.isPresent()) {
            return new DedupResult(Outcome.ALREADY_SEEN, bySource.get());
        }
        if (candidate.getNormalizedHash() != null) {
            List<JobPosting> byHash = repository.findByNormalizedHash(candidate.getNormalizedHash());
            if (!byHash.isEmpty()) {
                return new DedupResult(Outcome.CROSS_SOURCE_DUPLICATE, byHash.get(0));
            }
        }
        return new DedupResult(Outcome.NEW, null);
    }

    /** Record that an existing posting was also seen on the candidate's source (§13). */
    public JobPosting mergeSourceHistory(JobPosting existing, JobPosting candidate) {
        String token = candidate.getSource().name() + ":" + candidate.getExternalId();
        if (!existing.getSourcesSeen().contains(token)) {
            existing.getSourcesSeen().add(token);
        }
        return repository.save(existing);
    }
}

