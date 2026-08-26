package com.jobbot.module.candidate.parse;

import com.jobbot.module.candidate.dto.ParsedResumeDTO;
import com.jobbot.module.candidate.dto.ParsedResumeDTO.DetectedEducationDTO;
import com.jobbot.module.candidate.dto.ParsedResumeDTO.DetectedExperienceDTO;
import com.jobbot.module.candidate.dto.ParsedResumeDTO.DetectedSkillDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobbot.module.ai.AiProvider;

/**
 * Deterministic, offline extraction of structured fields from resume text (spec §5).
 * Intentionally conservative: it never fabricates data and never assigns EXPERT
 * proficiency. Everything it returns is a "detected" suggestion the user must verify.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeExtractionService {

    private final SkillNormalizer skillNormalizer;
    private final AiProvider aiProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE =
            Pattern.compile("(\\+?\\d[\\d\\s-]{8,}\\d)");
    // e.g. "2.8 years", "3 yrs", "5+ years"
    private static final Pattern YEARS =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\+?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);

    public ParsedResumeDTO extract(String fileName, String mimeType, long size, String checksum,
                                   String storagePath, String text) {
        String safe = text == null ? "" : text;
        String preview = safe.length() > 1200 ? safe.substring(0, 1200) : safe;
        
        if (aiProvider.isAvailable()) {
            try {
                String json = aiProvider.extractResume(safe);
                if (json != null) {
                    JsonNode root = mapper.readTree(json);
                    
                    String name = root.path("name").asText(null);
                    String email = root.path("email").asText(null);
                    String phone = root.path("phone").asText(null);
                    String summary = root.path("summary").asText(null);
                    
                    List<DetectedSkillDTO> skills = new ArrayList<>();
                    if (root.path("skills").isArray()) {
                        for (JsonNode sn : root.path("skills")) {
                            List<String> evidence = new ArrayList<>();
                            evidence.add("Detected by AI");
                            skills.add(new DetectedSkillDTO(
                                sn.path("name").asText(""), 
                                sn.path("category").asText(null), 
                                "STRONG", 
                                evidence));
                        }
                    }
                    
                    List<DetectedExperienceDTO> experience = new ArrayList<>();
                    if (root.path("experience").isArray()) {
                        for (JsonNode en : root.path("experience")) {
                            List<String> techs = new ArrayList<>();
                            if (en.path("technologies").isArray()) {
                                en.path("technologies").forEach(t -> techs.add(t.asText()));
                            }
                            experience.add(new DetectedExperienceDTO(
                                en.path("company").asText(null),
                                en.path("role").asText(null),
                                en.path("startDate").asText(null),
                                en.path("endDate").asText(null),
                                en.path("current").asBoolean(false),
                                techs
                            ));
                        }
                    }
                    
                    List<DetectedEducationDTO> education = new ArrayList<>();
                    if (root.path("education").isArray()) {
                        for (JsonNode edn : root.path("education")) {
                            education.add(new DetectedEducationDTO(
                                null, edn.path("degree").asText(null), null, null, null
                            ));
                        }
                    }
                    
                    List<String> projects = new ArrayList<>();
                    if (root.path("projects").isArray()) {
                        root.path("projects").forEach(p -> projects.add(p.asText()));
                    }
                    
                    log.info("Successfully extracted resume using Gemini AI");
                    return new ParsedResumeDTO(
                            fileName, mimeType, size, checksum, storagePath,
                            name, email, phone, summary,
                            skills, experience, education, projects, preview
                    );
                }
            } catch (Exception e) {
                log.warn("AI extraction failed, falling back to regex: {}", e.getMessage());
            }
        }

        String email = firstMatch(EMAIL, safe);
        String phone = cleanPhone(firstMatch(PHONE, safe));
        String name = guessName(safe, email);
        String summary = guessSummary(safe);

        List<DetectedSkillDTO> skills = detectSkills(safe);
        List<DetectedExperienceDTO> experience = detectExperience(safe);
        List<DetectedEducationDTO> education = detectEducation(safe);
        List<String> projects = detectProjects(safe);

        return new ParsedResumeDTO(
                fileName, mimeType, size, checksum, storagePath,
                name, email, phone, summary,
                skills, experience, education, projects, preview
        );
    }

    private List<DetectedSkillDTO> detectSkills(String text) {
        var detected = skillNormalizer.detectInText(text);
        List<DetectedSkillDTO> out = new ArrayList<>();
        for (var e : detected.entrySet()) {
            // Default to STRONG — the user can adjust proficiency after import.
            List<String> evidence = new ArrayList<>();
            evidence.add("Mentioned in resume");
            out.add(new DetectedSkillDTO(e.getKey(), e.getValue(), "STRONG", evidence));
        }
        return out;
    }

    private List<DetectedExperienceDTO> detectExperience(String text) {
        List<DetectedExperienceDTO> out = new ArrayList<>();
        // Heuristic: lines containing an employment date range and a role/company hint.
        // e.g. "Software Engineer, Acme Corp  Jan 2022 - Present"
        Pattern line = Pattern.compile(
                "(?m)^(.{3,80}?)\\s+((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\d{4}|\\d{1,2}/\\d{4})\\s*[-–to]+\\s*(Present|Current|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\d{4}|\\d{1,2}/\\d{4})",
                Pattern.CASE_INSENSITIVE);
        Matcher m = line.matcher(text);
        int guard = 0;
        while (m.find() && guard++ < 12) {
            String label = m.group(1).trim();
            String start = m.group(2).trim();
            String end = m.group(3).trim();
            boolean current = end.toLowerCase().startsWith("present") || end.toLowerCase().startsWith("current");

            String role = label;
            String company = null;
            for (String sep : new String[]{" at ", ", ", " - ", " | "}) {
                int idx = label.toLowerCase().indexOf(sep);
                if (idx > 0) {
                    role = label.substring(0, idx).trim();
                    company = label.substring(idx + sep.length()).trim();
                    break;
                }
            }
            var techs = new ArrayList<>(skillNormalizer.detectInText(label).keySet());
            out.add(new DetectedExperienceDTO(company, role, start, end, current, techs));
        }
        return out;
    }

    private List<DetectedEducationDTO> detectEducation(String text) {
        List<DetectedEducationDTO> out = new ArrayList<>();
        Pattern edu = Pattern.compile(
                "(B\\.?Tech|B\\.?E\\.?|M\\.?Tech|M\\.?E\\.?|B\\.?Sc|M\\.?Sc|MCA|BCA|MBA|Bachelor|Master)[^\\n]{0,80}",
                Pattern.CASE_INSENSITIVE);
        Matcher m = edu.matcher(text);
        int guard = 0;
        while (m.find() && guard++ < 5) {
            String degreeLine = m.group().trim();
            Integer endYear = null;
            Matcher ym = Pattern.compile("(19|20)\\d{2}").matcher(degreeLine);
            while (ym.find()) endYear = Integer.parseInt(ym.group());
            out.add(new DetectedEducationDTO(null, degreeLine, null, null, endYear));
        }
        return out;
    }

    private List<String> detectProjects(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?im)^\\s*(?:project|projects)\\s*[:\\-]?\\s*(.{3,100})$")
                .matcher(text);
        int guard = 0;
        while (m.find() && guard++ < 8) {
            String p = m.group(1).trim();
            if (!p.isBlank()) out.add(p);
        }
        return out;
    }

    // --- helpers ---

    private String firstMatch(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    private String cleanPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^\\d+]", "");
        return digits.length() >= 10 ? phone.trim() : null;
    }

    private String guessName(String text, String email) {
        // Heuristic: first non-empty line that looks like a person's name.
        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (EMAIL.matcher(line).find() || PHONE.matcher(line).find()) continue;
            if (line.length() > 40) continue;
            String[] words = line.split("\\s+");
            if (words.length >= 2 && words.length <= 4
                    && line.matches("[A-Za-z .'-]+")) {
                return line;
            }
            break; // only consider the top of the document
        }
        // Fallback: derive from email local-part
        if (email != null) {
            String local = email.substring(0, email.indexOf('@')).replaceAll("[._]", " ");
            return capitalizeWords(local);
        }
        return null;
    }

    private String guessSummary(String text) {
        Matcher m = Pattern.compile(
                "(?is)(?:summary|profile|objective|about)\\s*[:\\n]\\s*(.{40,400})")
                .matcher(text);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    private String capitalizeWords(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.trim().split("\\s+")) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
                    .append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}

