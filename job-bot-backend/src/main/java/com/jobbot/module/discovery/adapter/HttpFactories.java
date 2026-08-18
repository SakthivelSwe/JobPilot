package com.jobbot.module.discovery.adapter;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/** Shared factory for outbound HTTP with sane connect/read timeouts (no hanging threads). */
final class HttpFactories {

    private HttpFactories() {}

    static ClientHttpRequestFactory withTimeouts(int connectMs, int readMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofMillis(connectMs));
        f.setReadTimeout(Duration.ofMillis(readMs));
        return f;
    }
}

