package com.jobbot.module.matching;

import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateSkill;
import com.jobbot.module.discovery.JobPosting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The deterministic 8-factor matching engine (spec §14). Combines the Phase-2
 * {@link LocationEngine} and {@link NoticePeriodEngine} with skill/experience/role/
 * salary/company scoring. No AI — verified candidate facts only (§54).
 */
@Service
@RequiredArgsConstructor
public class JobMatchService {

    // Weights must sum to 100 (spec §14).
    static final int W_TECHNICAL = 35;
    static final int W_EXPERIENCE = 20;
    static final int W_ROLE = 10;
    static final int W_LOCATION = 10;
    static final int W_WORKMODE = 5;
    static final int W_NOTICE = 10;
    static final int W_SALARY = 5;
    static final int W_COMPANY = 5;

    private final LocationEngine locationEngine;
    private final NoticePeriodEngine noticeEngine;
    private final RecommendationEngine recommendationEngine;

    public MatchResult match(CandidateProfile profile, JobPosting job) {
        Set<String> candidateSkills = profile.getSkills() == null ? Set.of()
                : profile.getSkills().stream().map(CandidateSkill::getName)
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.toSet());

        String haystack = buildHaystack(job);
        List<String> riskFactors = new ArrayList<>();

        // 1. Technical
        List<String> matchedSkills = candidateSkills.stream()
                .filter(s -> containsWord(haystack, s)).sorted().collect(Collectors.toList());
        List<String> missingRequired = new ArrayList<>();
        List<String> preferredGaps = new ArrayList<>();
        int technical = technicalScore(job, candidateSkills, matchedSkills, missingRequired, preferredGaps);
        if (!missingRequired.isEmpty()) {
            riskFactors.add("Missing required skills: " + String.join(", ", missingRequired));
        }

        // 2. Experience
        Double yoe = profile.getYearsOfExperience() == null ? null
                : profile.getYearsOfExperience().doubleValue();
        int experience = experienceScore(yoe, job.getMinimumExperience(), job.getMaximumExperience());
        boolean experienceHardMismatch = isExperienceHardMismatch(yoe, job.getMinimumExperience(), job.getMaximumExperience());
        if (experienceHardMismatch) {
            riskFactors.add("Experience outside the job's range");
        }

        // 3. Role
        int role = roleScore(profile.getTargetRoles(), job.getTitle());

        // 4. Location
        int location = locationScore(profile, job);
        if (location <= 20) riskFactors.add("Location not compatible");

        // 5. Work mode
        int workMode = workModeScore(profile.getPreferredWorkModes(), job.getRemoteType());

        // 6. Notice
        int notice = noticeScore(profile, job);

        // 7. Salary
        int salary = salaryScore(profile, job);

        // 8. Company preference
        boolean excludedCompany = containsIgnoreCase(profile.getExcludedCompanies(), job.getCompany());
        int company = companyScore(profile, job, excludedCompany);
        if (excludedCompany) riskFactors.add("Excluded company");

        int overall = (int) Math.round((
                technical * W_TECHNICAL
                + experience * W_EXPERIENCE
                + role * W_ROLE
                + location * W_LOCATION
                + workMode * W_WORKMODE
                + notice * W_NOTICE
                + salary * W_SALARY
                + company * W_COMPANY) / 100.0);

        boolean missingAllRequired = job.getRequiredSkills() != null
                && !job.getRequiredSkills().isEmpty()
                && missingRequired.size() == job.getRequiredSkills().size();

        Recommendation recommendation = recommendationEngine.recommend(
                overall, excludedCompany, experienceHardMismatch, missingAllRequired);

        return new MatchResult(overall, technical, experience, role, location, workMode,
                notice, salary, company, matchedSkills, missingRequired, preferredGaps,
                riskFactors, recommendation);
    }

    // ---- factor scorers ----

    int technicalScore(JobPosting job, Set<String> candidateSkills, List<String> matchedSkills,
                       List<String> missingRequiredOut, List<String> preferredGapsOut) {
        List<String> required = job.getRequiredSkills();
        String haystack = buildHaystack(job);

        if (required != null && !required.isEmpty()) {
            int covered = 0;
            for (String req : required) {
                if (candidateHas(candidateSkills, req)) covered++;
                else missingRequiredOut.add(req);
            }
            if (job.getPreferredSkills() != null) {
                for (String pref : job.getPreferredSkills()) {
                    if (!candidateHas(candidateSkills, pref)) preferredGapsOut.add(pref);
                }
            }
            return (int) Math.round(100.0 * covered / required.size());
        }
        // No explicit required list → reward candidate-skill overlap with the JD text.
        if (candidateSkills.isEmpty()) return 0;
        long hits = candidateSkills.stream().filter(s -> containsWord(haystack, s)).count();
        return (int) Math.min(100, hits * 20); // 5 overlapping skills → 100
    }

    int experienceScore(Double yoe, Integer min, Integer max) {
        if (min == null && max == null) return 70;   // unknown → neutral
        if (yoe == null) return 60;                    // candidate unknown but job specifies
        double lo = min != null ? min : 0;
        double hi = max != null ? max : Double.MAX_VALUE;
        if (yoe >= lo && yoe <= hi) return 100;
        if (yoe < lo) return (int) Math.max(0, 100 - (lo - yoe) * 20);
        return (int) Math.max(40, 100 - (yoe - hi) * 10); // overqualified: mild penalty
    }

    boolean isExperienceHardMismatch(Double yoe, Integer min, Integer max) {
        if (yoe == null || (min == null && max == null)) return false;
        if (min != null && yoe < min - 2) return true;   // >2y under the floor
        if (max != null && yoe > max + 4) return true;    // well over the ceiling
        return false;
    }

    int roleScore(List<String> targetRoles, String title) {
        if (targetRoles == null || targetRoles.isEmpty() || title == null) return 60;
        Set<String> titleTokens = tokens(title);
        int best = 0;
        for (String role : targetRoles) {
            Set<String> roleTokens = tokens(role);
            if (roleTokens.isEmpty()) continue;
            long overlap = roleTokens.stream().filter(titleTokens::contains).count();
            int pct = (int) Math.round(100.0 * overlap / roleTokens.size());
            best = Math.max(best, pct);
        }
        return best;
    }

    int locationScore(CandidateProfile profile, JobPosting job) {
        WorkMode mode = parseMode(job.getRemoteType());
        boolean acceptsRemote = acceptsRemote(profile);
        boolean relocate = openToRelocation(profile);
        LocationMatch m = locationEngine.match(profile.getPreferredLocations(),
                acceptsRemote, relocate, job.getLocation(), mode);
        return switch (m) {
            case EXACT, CITY, REMOTE -> 100;
            case COUNTRY -> 70;
            case RELOCATION, UNKNOWN -> 60;
            case NONE -> 20;
        };
    }

    int workModeScore(List<String> preferredModes, String remoteType) {
        WorkMode jobMode = parseMode(remoteType);
        if (jobMode == WorkMode.UNKNOWN) return 60;
        if (preferredModes == null || preferredModes.isEmpty()) return 70;
        boolean accepted = preferredModes.stream()
                .anyMatch(p -> p != null && p.trim().equalsIgnoreCase(jobMode.name()));
        return accepted ? 100 : 40;
    }

    int noticeScore(CandidateProfile profile, JobPosting job) {
        String requirement = extractNoticeRequirement(job.getDescription());
        NoticeCompatibility c = noticeEngine.classify(
                profile.getNoticePeriodDays(), profile.getLastWorkingDate(), requirement, LocalDate.now());
        return switch (c) {
            case COMPATIBLE -> 100;
            case RECRUITER_APPROVAL -> 70;
            case MAJOR_MISMATCH -> 30;
            case UNKNOWN -> 70;
        };
    }

    int salaryScore(CandidateProfile profile, JobPosting job) {
        if (job.getSalary() == null) return 70; // usually not published → neutral
        BigDecimal floor = profile.getMinimumSalary() != null ? profile.getMinimumSalary()
                : profile.getExpectedSalary();
        if (floor == null) return 70;
        return job.getSalary().compareTo(floor) >= 0 ? 100 : 45;
    }

    int companyScore(CandidateProfile profile, JobPosting job, boolean excluded) {
        if (excluded) return 0;
        if (containsIgnoreCase(profile.getPreferredCompanies(), job.getCompany())) return 100;
        return 70;
    }

    // ---- helpers ----

    private String extractNoticeRequirement(String description) {
        if (description == null) return null;
        String d = description.toLowerCase(Locale.ROOT);
        // Only treat as a notice signal if the text is actually about joining/notice.
        if (d.contains("immediate joiner") || d.contains("immediate joining")) return "immediate";
        if (d.contains("notice period")) {
            // Pull a nearby "N days/month" via the engine's parser over a short window.
            int idx = d.indexOf("notice period");
            String window = d.substring(idx, Math.min(d.length(), idx + 40));
            return window;
        }
        return null;
    }

    private boolean candidateHas(Set<String> candidateSkills, String required) {
        if (required == null || required.isBlank()) return false;
        String r = required.toLowerCase(Locale.ROOT).trim();
        return candidateSkills.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).trim().equals(r));
    }

    private boolean acceptsRemote(CandidateProfile p) {
        if (p.getPreferredWorkModes() != null
                && p.getPreferredWorkModes().stream().anyMatch(m -> "REMOTE".equalsIgnoreCase(m))) {
            return true;
        }
        String rp = p.getRemotePreference();
        return rp != null && (rp.toLowerCase(Locale.ROOT).contains("remote")
                || rp.equalsIgnoreCase("yes") || rp.equalsIgnoreCase("true"));
    }

    private boolean openToRelocation(CandidateProfile p) {
        String r = p.getRelocationPreference();
        if (r == null) return false;
        String t = r.toLowerCase(Locale.ROOT);
        return t.contains("yes") || t.contains("open") || t.contains("flex") || t.equals("true");
    }

    private WorkMode parseMode(String remoteType) {
        if (remoteType == null) return WorkMode.UNKNOWN;
        try {
            return WorkMode.valueOf(remoteType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WorkMode.UNKNOWN;
        }
    }

    private String buildHaystack(JobPosting job) {
        StringBuilder sb = new StringBuilder();
        if (job.getTitle() != null) sb.append(job.getTitle()).append(' ');
        if (job.getDescription() != null) sb.append(job.getDescription()).append(' ');
        if (job.getRequiredSkills() != null) sb.append(String.join(" ", job.getRequiredSkills())).append(' ');
        if (job.getPreferredSkills() != null) sb.append(String.join(" ", job.getPreferredSkills()));
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private boolean containsWord(String haystackLower, String term) {
        if (term == null || term.isBlank()) return false;
        return Pattern.compile("\\b" + Pattern.quote(term.toLowerCase(Locale.ROOT).trim()) + "\\b")
                .matcher(haystackLower).find();
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        if (list == null || value == null) return false;
        return list.stream().anyMatch(s -> s != null && s.trim().equalsIgnoreCase(value.trim()));
    }

    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");

    private Set<String> tokens(String s) {
        if (s == null) return Set.of();
        return java.util.Arrays.stream(NON_WORD.split(s.toLowerCase(Locale.ROOT)))
                .filter(t -> t.length() > 1)
                .filter(t -> !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "developer", "engineer", "senior", "junior",
            "sr", "jr", "lead", "ii", "iii");
}

