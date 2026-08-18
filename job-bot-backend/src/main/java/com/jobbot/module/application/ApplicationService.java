package com.jobbot.module.application;

import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.activity.ActivityService;
import com.jobbot.module.application.dto.ApplicationCreateDTO;
import com.jobbot.module.job.Job;
import com.jobbot.module.job.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private static final List<String> KANBAN_STATUSES =
            List.of("applied", "viewed", "shortlisted", "interview", "offer", "rejected");

    /** Ordered active pipeline ladder (index = progression). Terminals handled separately. */
    private static final List<String> LADDER =
            List.of("applied", "viewed", "shortlisted", "interview", "offer");
    private static final java.util.Set<String> TERMINALS =
            java.util.Set.of("rejected", "withdrawn", "declined");

    private final ApplicationRepository repository;
    private final JobService jobService;
    private final ActivityService activity;

    public Page<Application> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Application findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new JobBotException("Application not found: " + id));
    }

    public boolean alreadyApplied(String company) {
        return company != null && repository.existsByCompanyIgnoreCase(company);
    }

    /** Record that the user applied to a job (human-in-the-loop). */
    public Application create(ApplicationCreateDTO dto) {
        Job job = jobService.findById(dto.getJobId());
        Application app = Application.builder()
                .jobId(job.getId())
                .resumeId(dto.getResumeId())
                .criteriaId(dto.getCriteriaId())
                .platform(job.getPlatform())
                .company(job.getCompany())
                .title(job.getTitle())
                .atsScore(job.getMatchScore())
                .matchedKeywords(job.getMatchKeywords())
                .missingKeywords(job.getMissingKeywords())
                .coverLetter(dto.getCoverLetter())
                .notes(dto.getNotes())
                .status("applied")
                .build();
        Application saved = repository.save(app);
        jobService.updateStatus(job.getId(), "applied");
        log.info("Recorded application to {} ({})", saved.getCompany(), saved.getTitle());
        activity.record("APPLICATION", "Applied to " + saved.getCompany(),
                saved.getTitle(), "application", saved.getId());
        return saved;
    }

    public Application updateStatus(UUID id, String status, String notes) {
        Application app = findById(id);
        String from = app.getStatus();
        validateTransition(from, status);
        app.setStatus(status);
        if (notes != null && !notes.isBlank()) {
            app.setNotes(notes);
        }
        Application saved = repository.save(app);
        if (from != null && !from.equalsIgnoreCase(status)) {
            activity.record("APPLICATION", saved.getCompany() + " → " + status,
                    saved.getTitle() + " (was " + from + ")", "application", saved.getId());
        }
        return saved;
    }

    /**
     * Canonical application state machine (spec §19). Allowed:
     *  - no-op (same status)
     *  - any → terminal (rejected/withdrawn/declined)
     *  - terminal → any active (reopen / correct a mistake)
     *  - forward or same-lane along the active ladder (skipping ahead is fine)
     * Rejected: backward moves among active stages (e.g. offer → applied).
     */
    void validateTransition(String from, String to) {
        if (from == null || to == null || from.equalsIgnoreCase(to)) return;
        String f = from.toLowerCase();
        String t = to.toLowerCase();
        if (TERMINALS.contains(t)) return;            // any → terminal
        if (TERMINALS.contains(f)) return;            // terminal → active (correction)
        int fi = LADDER.indexOf(f);
        int ti = LADDER.indexOf(t);
        if (fi < 0 || ti < 0) return;                 // unknown status → don't block
        if (ti < fi) {
            throw new JobBotException(
                    "Invalid transition: an application at '" + from + "' cannot move back to '" + to
                            + "'. Move it to a later stage or a terminal outcome (rejected/withdrawn).");
        }
    }

    public Application setInterview(UUID id, java.time.OffsetDateTime date, Integer round) {
        Application app = findById(id);
        app.setInterviewDate(date);
        if (round != null) app.setInterviewRound(round);
        app.setStatus("interview");
        return repository.save(app);
    }

    public Map<String, List<Application>> getKanban() {
        Map<String, List<Application>> board = new LinkedHashMap<>();
        for (String s : KANBAN_STATUSES) {
            board.put(s, new java.util.ArrayList<>());
        }
        for (Application app : repository.findAll()) {
            board.computeIfAbsent(app.getStatus(), k -> new java.util.ArrayList<>()).add(app);
        }
        return board;
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new JobBotException("Application not found: " + id);
        }
        repository.deleteById(id);
    }

    public ApplicationRepository repository() {
        return repository;
    }
}
