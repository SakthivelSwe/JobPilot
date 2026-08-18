package com.jobbot.module.pack;

import com.jobbot.module.ats.dto.AtsResult;
import com.jobbot.module.job.Job;
import com.jobbot.module.resume.Resume;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates suggested answers to common application screening questions,
 * deterministically from the resume + job. You review and edit before submitting.
 */
@Service
public class AnswersService {

    public List<Map<String, String>> generate(Job job, Resume resume, AtsResult ats) {
        String company = job.getCompany() != null ? job.getCompany() : "the company";
        String title = job.getTitle() != null ? job.getTitle() : "this role";
        List<String> matched = ats != null ? ats.getMatchedKeywords() : List.of();
        String skills = matched.isEmpty()
                ? String.join(", ", resume.getTargetSkills())
                : String.join(", ", matched);

        List<Map<String, String>> qa = new ArrayList<>();

        qa.add(qaOf("Why are you interested in this role?",
                "I'm excited about " + title + " at " + company + " because it aligns strongly with my experience in "
                        + skills + ", and I'm keen to contribute to impactful, scalable systems."));

        qa.add(qaOf("What is your notice period?",
                "I can serve a 30-day notice period and am open to discussing an earlier start if needed."));

        qa.add(qaOf("What is your expected CTC?",
                "I'm open to a competitive package in line with market standards for this role; happy to align to your band."));

        qa.add(qaOf("Are you willing to work from " + (job.getLocation() != null ? job.getLocation() : "the listed location") + "?",
                "Yes — I'm flexible on location for the right opportunity, including hybrid or remote arrangements."));

        if (!matched.isEmpty()) {
            qa.add(qaOf("Describe your experience with " + matched.get(0) + ".",
                    "I have hands-on production experience with " + matched.get(0)
                            + ", having used it to build and ship reliable features end to end."));
        }

        if (ats != null && ats.getMissingKeywords() != null && !ats.getMissingKeywords().isEmpty()) {
            String gap = ats.getMissingKeywords().get(0);
            qa.add(qaOf("How would you handle " + gap + " (not on your resume)?",
                    "While " + gap + " isn't my primary tool, I ramp up quickly on new technologies and have adjacent "
                            + "experience that transfers directly; I'm confident I can become productive fast."));
        }

        return qa;
    }

    private Map<String, String> qaOf(String q, String a) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("question", q);
        m.put("answer", a);
        return m;
    }
}

