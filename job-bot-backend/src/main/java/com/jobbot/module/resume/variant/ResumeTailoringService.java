package com.jobbot.module.resume.variant;

import com.jobbot.module.candidate.CandidateProfile;
import com.jobbot.module.candidate.CandidateSkill;
import com.jobbot.module.candidate.Project;
import com.jobbot.module.candidate.WorkExperience;
import com.jobbot.module.discovery.JobPosting;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resume tailoring (spec §21). Reorders and emphasizes the master profile's verified
 * facts to fit a job + variant. STRICTLY no fabrication — every output element is
 * traceable to the candidate profile.
 */
@Service
public class ResumeTailoringService {

    public TailoredResume tailor(CandidateProfile profile, JobPosting job, ResumeVariant variant) {
        String haystack = haystack(job);

        // Skills: only the candidate's own, reordered by (relevant-to-job, variant-priority).
        List<String> priority = variant.prioritySkills();
        List<String> orderedSkills = profile.getSkills() == null ? List.of()
                : profile.getSkills().stream()
                .map(CandidateSkill::getName)
                .filter(n -> n != null && !n.isBlank())
                .sorted(Comparator
                        .comparingInt((String n) -> containsWord(haystack, n) ? 0 : 1)     // job-relevant first
                        .thenComparingInt(n -> priorityIndex(priority, n))                  // then variant priority
                        .thenComparing(n -> n.toLowerCase(Locale.ROOT)))
                .toList();

        // Emphasized keywords = candidate skills that are both job-relevant and variant-priority.
        List<String> emphasized = orderedSkills.stream()
                .filter(n -> containsWord(haystack, n))
                .filter(n -> priorityIndex(priority, n) < Integer.MAX_VALUE)
                .toList();

        // Projects: reorder by count of technologies relevant to the job (verified tech only).
        List<TailoredResume.TailoredProject> projects = profile.getProjects() == null ? List.of()
                : profile.getProjects().stream()
                .sorted(Comparator.comparingInt((Project p) -> relevantTechCount(p.getTechnologies(), haystack)).reversed())
                .map(p -> new TailoredResume.TailoredProject(p.getName(), safe(p.getTechnologies())))
                .toList();

        // Experience: current first, then by tech overlap (verified facts only).
        List<TailoredResume.TailoredExperience> experiences = profile.getExperiences() == null ? List.of()
                : profile.getExperiences().stream()
                .sorted(Comparator
                        .comparing((WorkExperience e) -> e.isCurrent() ? 0 : 1)
                        .thenComparing(e -> -relevantTechCount(e.getTechnologies(), haystack)))
                .map(e -> new TailoredResume.TailoredExperience(
                        e.getCompany(), e.getRole(), e.isCurrent(), safe(e.getTechnologies())))
                .toList();

        String summary = buildSummary(profile, variant, emphasized);
        String title = variant.roleTarget();

        return new TailoredResume(profile.getId(), variant, variant.roleTarget(), title,
                summary, orderedSkills, projects, experiences, emphasized);
    }

    // ---- helpers ----

    /** Summary uses only verified facts: real years of experience + real skills. */
    private String buildSummary(CandidateProfile profile, ResumeVariant variant, List<String> emphasized) {
        String years = profile.getYearsOfExperience() != null
                ? trimZeros(profile.getYearsOfExperience().toPlainString()) + " years"
                : "Experienced";
        String core = emphasized.isEmpty() ? ""
                : "; core strengths: " + String.join(", ", emphasized.subList(0, Math.min(5, emphasized.size())));
        String emphasis = years + " targeting " + variant.roleTarget() + core + ".";
        String base = profile.getSummary();
        return (base != null && !base.isBlank()) ? base.trim() + "\n\n" + emphasis : emphasis;
    }

    private int priorityIndex(List<String> priority, String skill) {
        for (int i = 0; i < priority.size(); i++) {
            if (priority.get(i).equalsIgnoreCase(skill.trim())) return i;
        }
        return Integer.MAX_VALUE;
    }

    private int relevantTechCount(List<String> tech, String haystack) {
        if (tech == null) return 0;
        return (int) tech.stream().filter(t -> containsWord(haystack, t)).count();
    }

    private List<String> safe(List<String> in) {
        return in == null ? List.of() : in;
    }

    private String haystack(JobPosting job) {
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

    private String trimZeros(String s) {
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }
}


