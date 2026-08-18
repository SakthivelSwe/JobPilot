package com.jobbot.module.account;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.candidate.CandidateProfileRepository;
import com.jobbot.module.candidate.ResumeSourceDocumentRepository;
import com.jobbot.module.criteria.CriteriaRepository;
import com.jobbot.module.discovery.JobPosting;
import com.jobbot.module.discovery.JobPostingRepository;
import com.jobbot.module.manualqueue.ManualQueueRepository;
import com.jobbot.module.role.TargetRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data export &amp; ownership (spec §69/§70). Lets the single user export their data as
 * CSV/JSON and reset the app. This is personal career data — the user owns it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository postingRepository;
    private final CandidateProfileRepository profileRepository;
    private final CriteriaRepository criteriaRepository;
    private final TargetRoleRepository targetRoleRepository;
    private final ManualQueueRepository manualQueueRepository;
    private final ResumeSourceDocumentRepository resumeDocRepository;

    // ---- CSV exports (§69) ----

    public String applicationsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,company,title,platform,status,atsScore,autoApplied,appliedAt\n");
        for (Application a : applicationRepository.findAll()) {
            sb.append(csv(a.getId())).append(',')
                    .append(csv(a.getCompany())).append(',')
                    .append(csv(a.getTitle())).append(',')
                    .append(csv(a.getPlatform())).append(',')
                    .append(csv(a.getStatus())).append(',')
                    .append(csv(a.getAtsScore())).append(',')
                    .append(a.isAutoApplied()).append(',')
                    .append(csv(a.getAppliedAt())).append('\n');
        }
        return sb.toString();
    }

    public String jobsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,source,title,company,location,remoteType,capability,matchScore,recommendation,url\n");
        for (JobPosting p : postingRepository.findAll()) {
            sb.append(csv(p.getId())).append(',')
                    .append(csv(p.getSource())).append(',')
                    .append(csv(p.getTitle())).append(',')
                    .append(csv(p.getCompany())).append(',')
                    .append(csv(p.getLocation())).append(',')
                    .append(csv(p.getRemoteType())).append(',')
                    .append(csv(p.getApplicationCapability())).append(',')
                    .append(csv(p.getMatchScore())).append(',')
                    .append(csv(p.getRecommendation())).append(',')
                    .append(csv(p.getSourceUrl())).append('\n');
        }
        return sb.toString();
    }

    // ---- Full JSON backup (§69) ----

    @Transactional(readOnly = true)
    public Map<String, Object> fullExport() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profiles", profileRepository.findAll());
        data.put("targetRoles", targetRoleRepository.findAll());
        data.put("criteria", criteriaRepository.findAll());
        data.put("applications", applicationRepository.findAll());
        data.put("postings", postingRepository.findAll());
        data.put("manualQueue", manualQueueRepository.findAll());
        return data;
    }

    // ---- Data ownership (§70) ----

    /** Delete all personal data (keeps source/company configuration). Returns counts. */
    @Transactional
    public Map<String, Long> resetPersonalData() {
        Map<String, Long> deleted = new LinkedHashMap<>();
        deleted.put("applications", applicationRepository.count());
        deleted.put("manualQueue", manualQueueRepository.count());
        deleted.put("postings", postingRepository.count());
        deleted.put("criteria", criteriaRepository.count());
        deleted.put("targetRoles", targetRoleRepository.count());
        deleted.put("resumeDocuments", resumeDocRepository.count());
        deleted.put("profiles", profileRepository.count());

        // Order matters least here (no hard FKs enforced across these in H2/PG via converters).
        applicationRepository.deleteAllInBatch();
        manualQueueRepository.deleteAllInBatch();
        postingRepository.deleteAllInBatch();
        criteriaRepository.deleteAllInBatch();
        targetRoleRepository.deleteAllInBatch();
        resumeDocRepository.deleteAllInBatch();
        profileRepository.deleteAll(); // cascade to child skills/experiences/etc.

        log.warn("Personal data reset: {}", deleted);
        return deleted;
    }

    /** Delete only stored resume file metadata (§70). */
    @Transactional
    public long deleteResumeFiles() {
        long n = resumeDocRepository.count();
        resumeDocRepository.deleteAllInBatch();
        return n;
    }

    // ---- helpers ----

    private String csv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}

