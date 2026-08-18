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
import java.util.Map;

/**
 * Local AI via Ollama (http://localhost:11434). Zero cost, runs on your machine.
 * Enable with app.ai.provider=ollama. Falls back gracefully if unreachable.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
@Slf4j
public class OllamaAiProvider implements AiProvider {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.ai.ollama.url}")
    private String baseUrl;

    @Value("${app.ai.ollama.model}")
    private String model;

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Ollama not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String enrich(String resumeText, String jobDescription) {
        try {
            String prompt = ("You are a concise career coach. In ONE sentence, note the single most "
                    + "important gap or strength when applying this resume to this job.\n\nRESUME:\n%s\n\nJOB:\n%s")
                    .formatted(safe(resumeText), safe(jobDescription));

            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(res.body());
            String text = root.path("response").asText("");
            return text.isBlank() ? null : text.trim();
        } catch (Exception e) {
            log.warn("Ollama enrich failed: {}", e.getMessage());
            return null;
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }
}


