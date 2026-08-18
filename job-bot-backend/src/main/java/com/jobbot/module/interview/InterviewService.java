package com.jobbot.module.interview;

import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.resume.Resume;
import com.jobbot.module.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Interview workspace (spec §16/§35). Built on the EXISTING interview-stage
 * applications — no new entity. The prep pack is generated DETERMINISTICALLY from the
 * application's own matched skills + linked résumé, and is explicitly labelled
 * "suggested preparation" (never presented as authoritative fact — rule 35/67).
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final Set<String> INTERVIEW_STAGES = Set.of("interview", "offer");

    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;

    public record InterviewRef(String applicationId, String company, String role, String status,
                               String scheduledAt, Integer round, boolean upcoming) {}

    public record PrepPack(String applicationId, String company, String role,
                           String scheduledAt, Integer round,
                           List<String> technicalTopics,
                           List<String> likelyQuestions,
                           List<String> behavioralQuestions,
                           List<String> questionsToAsk,
                           List<ChecklistItem> checklist,
                           String note) {}

    public record ChecklistItem(String label, boolean done) {}

    /** All applications currently in an interview/offer stage, upcoming first. */
    public List<InterviewRef> list() {
        OffsetDateTime now = OffsetDateTime.now();
        List<InterviewRef> out = new ArrayList<>();
        for (Application a : applicationRepository.findAll()) {
            if (a.getStatus() == null || !INTERVIEW_STAGES.contains(a.getStatus().toLowerCase(Locale.ROOT))) continue;
            boolean upcoming = a.getInterviewDate() != null && a.getInterviewDate().isAfter(now);
            out.add(new InterviewRef(
                    a.getId().toString(), a.getCompany(), a.getTitle(), a.getStatus(),
                    a.getInterviewDate() != null ? a.getInterviewDate().toString() : null,
                    a.getInterviewRound(), upcoming));
        }
        // Upcoming (soonest) first, then the rest.
        out.sort((x, y) -> {
            if (x.upcoming() != y.upcoming()) return x.upcoming() ? -1 : 1;
            if (x.scheduledAt() == null) return 1;
            if (y.scheduledAt() == null) return -1;
            return x.scheduledAt().compareTo(y.scheduledAt());
        });
        return out;
    }

    /** Deterministic, clearly-suggested prep pack for one interview-stage application. */
    public PrepPack prep(UUID applicationId) {
        Application a = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new com.jobbot.common.exception.JobBotException(
                        "Application not found: " + applicationId));

        List<String> topics = deriveTopics(a);
        List<String> likely = new ArrayList<>();
        for (String t : topics.subList(0, Math.min(topics.size(), 6))) {
            likely.add("Describe a project where you used " + t + ".");
            likely.add("How would you troubleshoot a production issue involving " + t + "?");
        }
        if (likely.isEmpty()) {
            likely.add("Walk me through a recent project and your specific contribution.");
            likely.add("What was the hardest technical problem you solved recently?");
        }

        List<String> behavioral = List.of(
                "Tell me about yourself.",
                "Why are you looking to change roles?",
                "Describe a time you disagreed with a teammate and how you resolved it.",
                "Tell me about a project you're proud of and why.",
                "How do you handle competing priorities under a deadline?");

        List<String> ask = List.of(
                "How is the team structured and who would I work most closely with?",
                "What does success look like in the first 90 days?",
                "What are the biggest technical challenges the team is facing?",
                "How does the team approach code review and testing?",
                "What growth or learning opportunities are available?");

        List<ChecklistItem> checklist = new ArrayList<>();
        checklist.add(new ChecklistItem("Research " + (a.getCompany() != null ? a.getCompany() : "the company"), false));
        checklist.add(new ChecklistItem("Re-read your résumé and the job description", false));
        for (String t : topics.subList(0, Math.min(topics.size(), 3))) {
            checklist.add(new ChecklistItem("Practice " + t, false));
        }
        checklist.add(new ChecklistItem("Prepare 2–3 questions to ask", false));

        return new PrepPack(
                a.getId().toString(), a.getCompany(), a.getTitle(),
                a.getInterviewDate() != null ? a.getInterviewDate().toString() : null,
                a.getInterviewRound(), topics, likely, behavioral, ask, checklist,
                "Suggested preparation — generated from your matched skills and this role. "
                        + "Adapt it to your own experience; nothing here is a guaranteed question.");
    }

    private List<String> deriveTopics(Application a) {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        if (a.getMatchedKeywords() != null) topics.addAll(a.getMatchedKeywords());
        if (topics.isEmpty() && a.getResumeId() != null) {
            Resume r = resumeRepository.findById(a.getResumeId()).orElse(null);
            if (r != null && r.getTargetSkills() != null) topics.addAll(r.getTargetSkills());
        }
        return new ArrayList<>(topics);
    }
}

