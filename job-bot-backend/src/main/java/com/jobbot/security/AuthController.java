package com.jobbot.security;

import com.jobbot.common.ApiResponse;
import com.jobbot.common.exception.JobBotException;
import com.jobbot.module.account.User;
import com.jobbot.module.account.UserRepository;
import com.jobbot.module.platform.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final LoginRateLimiter rateLimiter;
    private final UserRepository userRepository;
    private final PlatformConfigService platformConfigService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public record LoginReq(String username, String password) {}
    public record RefreshReq(String refreshToken) {}
    public record TokenResp(String token, String refreshToken, String type) {}

    @PostMapping("/login")
    public ApiResponse<TokenResp> login(@RequestBody LoginReq req) {
        if (req == null || req.username() == null || req.password() == null) {
            throw new JobBotException("username and password required");
        }
        String username = req.username().trim();
        if (username.isEmpty()) {
            throw new JobBotException("username cannot be empty");
        }
        
        if (rateLimiter.isLocked(username)) {
            throw new JobBotException("Too many failed attempts. Try again later.");
        }

        Optional<User> existingUser = userRepository.findByUsername(username);
        User user;

        if (existingUser.isEmpty()) {
            // First time login: create account
            String hash = encoder.encode(req.password());
            user = new User(username, hash);
            user = userRepository.save(user);
            log.info("Created new user account: {}", username);
            // Seed per-user platform configs (NAUKRI, LINKEDIN, INDEED) with the correct tenant.
            seedPlatformsForUser(user.getId());
        } else {
            // Existing user: verify password
            user = existingUser.get();
            if (!encoder.matches(req.password(), user.getPasswordHash())) {
                rateLimiter.recordFailure(username);
                throw new JobBotException("Invalid credentials");
            }
            // Seed platform configs if the user somehow has none (e.g., pre-existing user)
            seedPlatformsForUser(user.getId());
        }

        rateLimiter.reset(username);
        return ApiResponse.ok(tokens(user.getId()), "Login OK");
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResp> refresh(@RequestBody RefreshReq req) {
        if (req == null || req.refreshToken() == null) {
            throw new JobBotException("refreshToken required");
        }
        try {
            var claims = jwtService.parse(req.refreshToken());
            if (!"refresh".equals(jwtService.type(claims))) {
                throw new JobBotException("Invalid refresh token");
            }
            String userId = claims.getSubject();
            return ApiResponse.ok(tokens(userId), "Refreshed");
        } catch (JobBotException e) {
            throw e;
        } catch (Exception e) {
            throw new JobBotException("Invalid or expired refresh token");
        }
    }

    private TokenResp tokens(String userId) {
        return new TokenResp(
                jwtService.generateAccess(userId, List.of("USER")),
                jwtService.generateRefresh(userId),
                "Bearer");
    }

    /**
     * Seeds the three default platform_config rows (NAUKRI, LINKEDIN, INDEED) scoped to
     * the given userId. A temporary security context is established so that Hibernate's
     * TenantResolver resolves to the correct user_id, not "system".
     */
    private void seedPlatformsForUser(String userId) {
        var prev = SecurityContextHolder.getContext().getAuthentication();
        try {
            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("seedPlatformsForUser: context set to userId={}, resolves to tenant={}",
                    userId, SecurityUtils.getCurrentUserId());
            platformConfigService.seedDefaults();
            log.info("Seeded platform configs for new user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to seed platform configs for user {}: {}", userId, e.getMessage());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(prev);
        }
    }

    @PostMapping("/hash")
    public ApiResponse<Map<String, String>> hash(@RequestBody Map<String, String> body) {
        String pw = body.getOrDefault("password", "");
        return ApiResponse.ok(Map.of("hash", encoder.encode(pw)));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/me")
    public ApiResponse<String> deleteAccount() {
        String userId = SecurityUtils.getCurrentUserId();
        if ("system".equals(userId)) {
            throw new JobBotException("Cannot delete system user");
        }

        log.info("Deleting user account and all associated data for userId: {}", userId);

        String[] tenantTables = {
            "ai_usage", "application", "candidate_profile", "company", "criteria",
            "job_posting", "job", "manual_queue_entry", "platform_config", "job_queue",
            "resume_source_document", "resume", "target_role", "activity"
        };

        for (String table : tenantTables) {
            try {
                jdbcTemplate.update("DELETE FROM " + table + " WHERE user_id = ?", userId);
            } catch (Exception e) {
                log.warn("Failed to delete data from table {}: {}", table, e.getMessage());
            }
        }

        userRepository.deleteById(userId);
        log.info("Successfully deleted user {}", userId);

        return ApiResponse.ok("Account deleted completely");
    }
}

