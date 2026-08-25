package com.jobbot.module.platform;

import com.jobbot.common.ApiResponse;
import com.jobbot.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for platform account session management.
 *
 * <p>POST /api/platform-config/{platform}/connect  — opens Playwright; user logs in
 * <p>GET  /api/platform-config/{platform}/session  — returns current session status
 * <p>POST /api/platform-config/{platform}/validate — re-probes the stored session
 * <p>POST /api/platform-config/{platform}/disconnect — revokes session + deletes file
 */
@RestController
@RequestMapping("/api/platform-config")
@RequiredArgsConstructor
public class PlatformSessionController {

    private final PlatformSessionService sessionService;
    private final PlatformConfigService  configService;

    /**
     * Triggers the Playwright login flow.
     * Opens a visible browser on the server machine (local-only feature).
     * The user completes login manually; session is saved once confirmed.
     */
    @PostMapping("/{platform}/connect")
    public ApiResponse<PlatformSessionDTO> connect(@PathVariable String platform) {
        String userId = SecurityUtils.getCurrentUserId();
        PlatformConfig config = sessionService.connectAccount(platform, userId);
        return ApiResponse.ok(toDto(config), "Account connected for " + platform.toUpperCase());
    }

    /**
     * Connects an account by manually providing the session cookie value.
     */
    @PostMapping("/{platform}/session-cookie")
    public ApiResponse<PlatformSessionDTO> connectManual(@PathVariable String platform, @RequestBody java.util.Map<String, String> body) {
        String userId = SecurityUtils.getCurrentUserId();
        String cookieValue = body.get("cookieValue");
        PlatformConfig config = sessionService.saveManualCookie(platform, userId, cookieValue);
        return ApiResponse.ok(toDto(config), "Account connected via manual cookie for " + platform.toUpperCase());
    }

    /** Returns the current session status without opening a browser. */
    @GetMapping("/{platform}/session")
    public ApiResponse<PlatformSessionDTO> session(@PathVariable String platform) {
        PlatformConfig config = configService.get(platform.toUpperCase());
        return ApiResponse.ok(toDto(config));
    }

    /**
     * Re-probes the platform to verify the stored session is still valid.
     * Updates sessionStatus to CONNECTED or EXPIRED accordingly.
     */
    @PostMapping("/{platform}/validate")
    public ApiResponse<PlatformSessionDTO> validate(@PathVariable String platform) {
        PlatformConfig config = sessionService.validateSession(platform);
        return ApiResponse.ok(toDto(config), "Session validated for " + platform.toUpperCase());
    }

    /**
     * Disconnects the account: deletes the local encrypted session file and
     * resets all session fields to DISCONNECTED defaults.
     */
    @PostMapping("/{platform}/disconnect")
    public ApiResponse<PlatformSessionDTO> disconnect(@PathVariable String platform) {
        PlatformConfig config = sessionService.disconnect(platform);
        return ApiResponse.ok(toDto(config), "Account disconnected for " + platform.toUpperCase());
    }

    /** Maps PlatformConfig ? a safe DTO that never exposes sessionFilePath. */
    private PlatformSessionDTO toDto(PlatformConfig c) {
        return new PlatformSessionDTO(
                c.getPlatformName(),
                c.getSessionStatus(),
                Boolean.TRUE.equals(c.getSessionActive()),
                c.getSessionUsername(),
                c.getSessionConnectedAt()
        );
    }
}

