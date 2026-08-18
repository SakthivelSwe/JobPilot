package com.jobbot.module.pack;

import com.jobbot.module.ai.AiProvider;
import com.jobbot.module.ats.dto.AtsResult;
import com.jobbot.module.job.Job;
import com.jobbot.module.resume.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generates a tailored cover letter deterministically (no AI required).
 * If an AI provider is enabled, it appends an enrichment note.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoverLetterService {

    private final AiProvider aiProvider;

    public String generate(Job job, Resume resume, AtsResult ats) {
        String company = job.getCompany() != null ? job.getCompany() : "your team";
        String title = job.getTitle() != null ? job.getTitle() : "this role";
        List<String> matched = ats != null ? ats.getMatchedKeywords() : List.of();
        String skills = matched.isEmpty()
                ? String.join(", ", resume.getTargetSkills())
                : String.join(", ", matched);
        String summary = resume.getExperienceSummary() != null
                ? resume.getExperienceSummary()
                : "a software engineer with hands-on production experience";

        StringBuilder sb = new StringBuilder();
        sb.append("Dear Hiring Manager,\n\n");
        sb.append("I am writing to express my strong interest in the ").append(title)
          .append(" position at ").append(company).append(". As ").append(summary)
          .append(", I believe my background aligns closely with what you are looking for.\n\n");

        if (!matched.isEmpty()) {
            sb.append("Your role calls for ").append(skills)
              .append(" — areas where I have delivered real, measurable results. ")
              .append("I have applied these skills to build reliable, scalable systems and would bring the same rigor to ")
              .append(company).append(".\n\n");
        } else {
            sb.append("I bring solid experience across ").append(skills)
              .append(", and I am confident I can quickly add value to your team.\n\n");
        }

        if (ats != null && ats.getBestResumeAngle() != null && !ats.getBestResumeAngle().isBlank()) {
            sb.append(ats.getBestResumeAngle()).append("\n\n");
        }

        sb.append("I would welcome the opportunity to discuss how my experience can contribute to ")
          .append(company).append("'s goals. Thank you for your time and consideration.\n\n");
        sb.append("Sincerely,\n").append(resume.getName());

        String letter = sb.toString();

        try {
            if (aiProvider.isAvailable()) {
                String note = aiProvider.enrich(resume.getResumeText(), job.getDescription());
                if (note != null && !note.isBlank()) {
                    letter = letter + "\n\n[AI suggestion: " + note + "]";
                }
            }
        } catch (Exception e) {
            log.warn("Cover letter AI enrichment skipped: {}", e.getMessage());
        }
        return letter;
    }
}

