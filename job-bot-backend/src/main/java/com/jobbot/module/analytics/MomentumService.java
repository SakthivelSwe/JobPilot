package com.jobbot.module.analytics;

import com.jobbot.module.activity.ActivityRepository;
import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Career Momentum (spec §22, rules 48/67) — a single, fully-explainable score for the
 * last 7 days, computed DETERMINISTICALLY from real persisted rows. Never random.
 *
 * <p>Formula (each factor capped, total capped at 100):
 * <pre>
 *   applicationsThisWeek  × 6, cap 5   → up to 30
 *   interviewsThisWeek    × 15, cap 2  → up to 30
 *   responsesThisWeek     × 5, cap 4   → up to 20
 *   activeDays            × 3, cap 7   → up to 20 (from the activity log)
 * </pre>
 * If there is essentially no activity, momentum is NOT reported ("not enough activity").
 */
@Service
@RequiredArgsConstructor
public class MomentumService {

    private static final int WINDOW_DAYS = 7;
    private static final Set<String> RESPONSE_STATUSES =
            Set.of("viewed", "shortlisted", "interview", "offer", "screening");
    private static final Set<String> INTERVIEW_STATUSES = Set.of("interview", "offer");

    private final ApplicationRepository applicationRepository;
    private final ActivityRepository activityRepository;

    public record Factor(String name, int value, int points, String detail) {}
    public record Momentum(boolean available, Integer score, String label,
                           int windowDays, List<Factor> factors, String message) {}

    public Momentum compute() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(WINDOW_DAYS);
        List<Application> apps = applicationRepository.findAll();

        int applications = (int) apps.stream()
                .filter(a -> a.getAppliedAt() != null && a.getAppliedAt().isAfter(since))
                .count();

        int interviews = (int) apps.stream()
                .filter(a -> a.getInterviewDate() != null && a.getInterviewDate().isAfter(since))
                .count();

        int responses = (int) apps.stream()
                .filter(a -> a.getStatus() != null
                        && RESPONSE_STATUSES.contains(a.getStatus().toLowerCase(Locale.ROOT))
                        && a.getLastUpdated() != null && a.getLastUpdated().isAfter(since))
                .count();

        // Distinct active days from the real activity log.
        Set<LocalDate> days = new HashSet<>();
        activityRepository.findByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 200))
                .forEach(e -> { if (e.getCreatedAt() != null && e.getCreatedAt().isAfter(since))
                        days.add(e.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate()); });
        int activeDays = days.size();

        int pApps = Math.min(applications, 5) * 6;
        int pInts = Math.min(interviews, 2) * 15;
        int pResp = Math.min(responses, 4) * 5;
        int pDays = Math.min(activeDays, 7) * 3;
        pDays = Math.min(pDays, 20);
        int score = Math.min(pApps + pInts + pResp + pDays, 100);

        List<Factor> factors = new ArrayList<>();
        factors.add(new Factor("Applications", applications, pApps, applications + " sent this week"));
        factors.add(new Factor("Interviews", interviews, pInts, interviews + " scheduled this week"));
        factors.add(new Factor("Responses", responses, pResp, responses + " moved forward"));
        factors.add(new Factor("Consistency", activeDays, pDays, activeDays + " active day(s)"));

        boolean anyActivity = applications + interviews + responses + activeDays > 0;
        if (!anyActivity) {
            return new Momentum(false, null, "No momentum yet", WINDOW_DAYS, factors,
                    "Not enough activity this week. Apply to a few roles to build momentum.");
        }

        String label = score >= 70 ? "Strong week"
                : score >= 40 ? "Building momentum"
                : "Quiet week";
        String message = summarise(applications, interviews, responses);
        return new Momentum(true, score, label, WINDOW_DAYS, factors, message);
    }

    private String summarise(int applications, int interviews, int responses) {
        List<String> parts = new ArrayList<>();
        if (applications > 0) parts.add("+" + applications + " application" + (applications == 1 ? "" : "s"));
        if (interviews > 0) parts.add("+" + interviews + " interview" + (interviews == 1 ? "" : "s"));
        if (responses > 0) parts.add("+" + responses + " response" + (responses == 1 ? "" : "s"));
        return parts.isEmpty() ? "Keep applying to build momentum." : String.join(" · ", parts) + " this week.";
    }
}



