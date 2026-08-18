package com.jobbot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${app.security.jwt.secret:change-me-change-me-change-me-change-me-change-me-1234}")
    private String secretRaw;

    @Value("${app.security.jwt.ttl-hours:168}")
    private long ttlHours;

    @Value("${app.security.jwt.access-ttl-minutes:60}")
    private long accessTtlMinutes;

    @Value("${app.security.jwt.refresh-ttl-hours:336}")
    private long refreshTtlHours;

    private SecretKey key;

    @PostConstruct
    void init() {
        // Accept either base64 or raw string >=32 chars
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretRaw);
            if (bytes.length < 32) throw new IllegalArgumentException("too short");
        } catch (Exception e) {
            bytes = secretRaw.getBytes();
        }
        if (bytes.length < 32) {
            // pad to 32 bytes
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generate(String subject, List<String> roles) {
        return generateAccess(subject, roles);
    }

    /** Short-lived access token (type=access). */
    public String generateAccess(String subject, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtlMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    /** Long-lived refresh token (type=refresh). Carries no roles. */
    public String generateRefresh(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTtlHours * 3600_000L))
                .signWith(key)
                .compact();
    }

    public String type(Claims c) {
        Object t = c.get("type");
        return t != null ? t.toString() : "access";
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> roles(Claims c) {
        Object r = c.get("roles");
        return r instanceof List ? (List<String>) r : List.of("USER");
    }
}

