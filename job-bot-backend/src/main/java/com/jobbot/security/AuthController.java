package com.jobbot.security;

import com.jobbot.common.ApiResponse;
import com.jobbot.common.exception.JobBotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Minimal auth: single admin user configured via env.
 * (The former machine "engine token" path was removed with the forbidden
 * automation services — spec §1.)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final LoginRateLimiter rateLimiter;

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    /** BCrypt hash of admin password. Default is bcrypt("changeme"). */
    @Value("${app.security.admin.password-hash:$2a$10$gH/.L3ULXaWW9j8u5k31j.PRRHdHGn.BmIvPOZnh9HvQR35fFQOAy}")
    private String adminHash;

    public record LoginReq(String username, String password) {}
    public record RefreshReq(String refreshToken) {}
    public record TokenResp(String token, String refreshToken, String type) {}

    @PostMapping("/login")
    public ApiResponse<TokenResp> login(@RequestBody LoginReq req) {
        if (req == null || req.username() == null || req.password() == null) {
            throw new JobBotException("username and password required");
        }
        String key = req.username();
        if (rateLimiter.isLocked(key)) {
            throw new JobBotException("Too many failed attempts. Try again later.");
        }
        if (!adminUsername.equals(req.username()) || !encoder.matches(req.password(), adminHash)) {
            rateLimiter.recordFailure(key);
            throw new JobBotException("Invalid credentials");
        }
        rateLimiter.reset(key);
        return ApiResponse.ok(tokens(adminUsername), "Login OK");
    }

    /** Exchange a valid refresh token for a fresh access + refresh pair (rotation). */
    @PostMapping("/refresh")
    public ApiResponse<TokenResp> refresh(@RequestBody RefreshReq req) {
        if (req == null || req.refreshToken() == null) {
            throw new JobBotException("refreshToken required");
        }
        try {
            var claims = jwtService.parse(req.refreshToken());
            if (!"refresh".equals(jwtService.type(claims)) || !adminUsername.equals(claims.getSubject())) {
                throw new JobBotException("Invalid refresh token");
            }
            return ApiResponse.ok(tokens(adminUsername), "Refreshed");
        } catch (JobBotException e) {
            throw e;
        } catch (Exception e) {
            throw new JobBotException("Invalid or expired refresh token");
        }
    }

    private TokenResp tokens(String subject) {
        return new TokenResp(
                jwtService.generateAccess(subject, List.of("ADMIN", "USER")),
                jwtService.generateRefresh(subject),
                "Bearer");
    }


    @PostMapping("/hash")
    public ApiResponse<Map<String, String>> hash(@RequestBody Map<String, String> body) {
        // Utility to generate a bcrypt hash for a chosen password.
        String pw = body.getOrDefault("password", "");
        return ApiResponse.ok(Map.of("hash", encoder.encode(pw)));
    }
}

