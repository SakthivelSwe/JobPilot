package com.jobbot.module.matching;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic notice-period engine (spec §16). Especially important for this
 * candidate. No AI, no external calls.
 *
 * <p>Per §16 a job is never auto-rejected here — this only classifies
 * compatibility; hard filtering is a user-configured decision elsewhere.
 */
@Component
public class NoticePeriodEngine {

    /** Slack (in days) within which an over-notice is treated as negotiable. */
    private static final int RECRUITER_APPROVAL_SLACK_DAYS = 30;

    private static final Pattern DAYS = Pattern.compile("(\\d+)\\s*days?");
    private static final Pattern MONTHS = Pattern.compile("(\\d+)\\s*month");

    /**
     * The candidate's effective available-in days.
     * Prefers a concrete {@code lastWorkingDate} (days from today, floored at 0);
     * otherwise falls back to {@code noticePeriodDays}. Returns null if neither known.
     */
    public Integer candidateAvailableInDays(Integer noticePeriodDays, LocalDate lastWorkingDate, LocalDate today) {
        if (lastWorkingDate != null) {
            long days = ChronoUnit.DAYS.between(today, lastWorkingDate);
            return (int) Math.max(0, days);
        }
        return noticePeriodDays;
    }

    /**
     * Parse a job's notice requirement into the maximum acceptable joining days.
     * Returns {@code null} for "Any"/"Unknown"/unparseable (meaning: no constraint).
     * Examples: "Immediate"→0, "15 days"→15, "1 month"→30, "Any"→null.
     */
    public Integer parseRequiredMaxDays(String requirement) {
        if (requirement == null) return null;
        String t = requirement.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.contains("any") || t.contains("unknown") || t.contains("flexible")) {
            return null;
        }
        if (t.contains("immediate") || t.contains("asap")) return 0;

        Matcher m = MONTHS.matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1)) * 30;
        m = DAYS.matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    /**
     * Classify compatibility.
     *
     * @param candidateAvailableInDays days until the candidate can join (see helper); null → UNKNOWN candidate side is still comparable via job side
     * @param jobMaxDays               job's max acceptable joining days; null → no constraint (COMPATIBLE)
     */
    public NoticeCompatibility classify(Integer candidateAvailableInDays, Integer jobMaxDays) {
        if (jobMaxDays == null) {
            // Job accepts any notice period.
            return NoticeCompatibility.COMPATIBLE;
        }
        if (candidateAvailableInDays == null) {
            // Job has a constraint but we don't know the candidate's availability.
            return NoticeCompatibility.UNKNOWN;
        }
        if (candidateAvailableInDays <= jobMaxDays) {
            return NoticeCompatibility.COMPATIBLE;
        }
        int over = candidateAvailableInDays - jobMaxDays;
        return over <= RECRUITER_APPROVAL_SLACK_DAYS
                ? NoticeCompatibility.RECRUITER_APPROVAL
                : NoticeCompatibility.MAJOR_MISMATCH;
    }

    /** Convenience: classify straight from raw candidate + job inputs. */
    public NoticeCompatibility classify(Integer noticePeriodDays, LocalDate lastWorkingDate,
                                        String jobRequirement, LocalDate today) {
        Integer available = candidateAvailableInDays(noticePeriodDays, lastWorkingDate, today);
        Integer jobMax = parseRequiredMaxDays(jobRequirement);
        return classify(available, jobMax);
    }
}

