package com.jobbot.module.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
@Slf4j
public class GeminiAiProvider implements AiProvider {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.ai.gemini.api-key}")
    private String apiKey;

    @Value("${app.ai.gemini.model}")
    private String model;

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String enrich(String resumeText, String jobDescription) {
        String prompt = ("You are a concise career coach. In ONE sentence, note the single most "
                + "important gap or strength when applying this resume to this job.\n\nRESUME:\n%s\n\nJOB:\n%s")
                .formatted(safe(resumeText), safe(jobDescription));
        
        return callGemini(prompt);
    }
    
    @Override
    public String extractResume(String text) {
        String prompt = """
            You are a highly accurate resume parser. I will provide you with the raw text of a resume.
            Extract the information into the following JSON format. ONLY return valid JSON without any markdown formatting or extra text.
            If a field is missing, omit it or set it to null.
            
            Format:
            {
              "name": "Full Name",
              "email": "Email Address",
              "phone": "Phone Number",
              "summary": "Professional Summary",
              "skills": [
                {
                  "name": "Skill Name",
                  "category": "e.g. LANGUAGE, TOOL, FRAMEWORK, SOFT_SKILL"
                }
              ],
              "experience": [
                {
                  "company": "Company Name",
                  "role": "Job Title",
                  "startDate": "Month Year",
                  "endDate": "Month Year or Present",
                  "current": true,
                  "technologies": ["tech1", "tech2"]
                }
              ],
              "education": [
                {
                  "degree": "Full degree text (e.g. B.S. Computer Science)"
                }
              ],
              "projects": ["Project Name 1", "Project Name 2"]
            }
            
            Resume Text:
            """ + safe(text);
            
        return callGemini(prompt);
    }
    
    private String callGemini(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Gemini API error: {} {}", res.statusCode(), res.body());
                return null;
            }
            
            JsonNode root = mapper.readTree(res.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText("");
                    text = text.trim();
                    // Remove markdown formatting if present
                    if (text.startsWith("```json")) text = text.substring(7);
                    if (text.startsWith("```")) text = text.substring(3);
                    if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
                    return text.trim();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            return null;
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.length() > 8000 ? s.substring(0, 8000) : s; // Gemini has a large context window
    }
}
