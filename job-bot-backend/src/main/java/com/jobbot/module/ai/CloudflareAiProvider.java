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

/**
 * Optional cloud AI via Cloudflare Workers AI (10k neurons/day free).
 * Enable with app.ai.provider=cloudflare and set CF_ACCOUNT_ID + CF_API_TOKEN.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "cloudflare")
@Slf4j
public class CloudflareAiProvider implements AiProvider {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.ai.cloudflare.account-id}")
    private String accountId;

    @Value("${app.ai.cloudflare.api-token}")
    private String apiToken;

    @Value("${app.ai.cloudflare.model}")
    private String model;

    @Override
    public boolean isAvailable() {
        return accountId != null && !accountId.isBlank()
                && apiToken != null && !apiToken.isBlank();
    }

    @Override
    public String enrich(String resumeText, String jobDescription) {
        if (!isAvailable()) return null;
        try {
            String url = "https://api.cloudflare.com/client/v4/accounts/"
                    + accountId + "/ai/run/" + model;

            Map<String, Object> body = Map.of("messages", List.of(
                    Map.of("role", "system", "content", "You are a concise career coach."),
                    Map.of("role", "user", "content",
                            "In ONE sentence, note the most important gap or strength applying this resume to this job.\n\nRESUME:\n"
                                    + safe(resumeText) + "\n\nJOB:\n" + safe(jobDescription))));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(res.body());
            String text = root.path("result").path("response").asText("");
            return text.isBlank() ? null : text.trim();
        } catch (Exception e) {
            log.warn("Cloudflare AI enrich failed: {}", e.getMessage());
            return null;
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }
}

