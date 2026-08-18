package com.jobbot.module.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default AI provider — does nothing. Keeps the system fully functional
 * and $0 without Ollama or Cloudflare. Active unless app.ai.provider is set
 * to another value.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "noop", matchIfMissing = true)
public class NoOpAiProvider implements AiProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String enrich(String resumeText, String jobDescription) {
        return null;
    }
}

