package com.jobbot.module.discovery;

import com.jobbot.module.platform.PlatformConfigService;
import org.springframework.stereotype.Service;

/**
 * Determines how a job may be applied to. India-market posture:
 *
 * <ul>
 *   <li>LINKEDIN → MANUAL_REQUIRED: never server-side automated; the Chrome
 *       Extension handles Easy Apply inside the user's own real browser session.</li>
 *   <li>NAUKRI / INDEED with active session → AUTO_ELIGIBLE: the local Playwright
 *       engine applies inside the user's real logged-in session (cookie-based).</li>
 *   <li>NAUKRI / INDEED without session → ASSISTED_APPLY: pack prepared, user submits.</li>
 *   <li>GREENHOUSE/ASHBY/LEVER/WORKABLE → ASSISTED_APPLY (pack prepared, user submits).</li>
 *   <li>Unknown / MANUAL → MANUAL_REQUIRED.</li>
 * </ul>
 */
@Service
public class ApplicationCapabilityService {

    private final PlatformConfigService platformConfigService;

    public ApplicationCapabilityService(PlatformConfigService platformConfigService) {
        this.platformConfigService = platformConfigService;
    }

    /** Safe default classification based purely on the source type. Ignores session state. */
    public ApplicationCapability determine(AtsType source) {
        return determine(source, false, false);
    }

    /**
     * Full determination — session-aware.
     *
     * @param source                the ATS/source
     * @param authorizedIntegration an officially authorized submission API is configured
     * @param userApprovedAuto      the user explicitly enabled + approved auto submission
     */
    public ApplicationCapability determine(AtsType source,
                                           boolean authorizedIntegration,
                                           boolean userApprovedAuto) {
        if (source == null) return ApplicationCapability.MANUAL_REQUIRED;

        // LinkedIn is never handled by the server-side application-engine.
        // The Chrome Extension applies inside the user's real browser session.
        if (source == AtsType.LINKEDIN) {
            return ApplicationCapability.MANUAL_REQUIRED;
        }

        // Naukri / Indeed: if the user has linked their account (session cookie on file),
        // the apply engine can log in as them — AUTO_ELIGIBLE.
        if (source == AtsType.NAUKRI || source == AtsType.INDEED) {
            if (isSessionActive(source.name())) {
                return ApplicationCapability.AUTO_ELIGIBLE;
            }
            return ApplicationCapability.ASSISTED_APPLY;
        }

        // Authorized ATS integration path (still requires user opt-in).
        if (authorizedIntegration && userApprovedAuto && supportsAuthorizedSubmission(source)) {
            return ApplicationCapability.AUTO_ELIGIBLE;
        }

        return switch (source) {
            case GREENHOUSE, ASHBY, LEVER, WORKABLE -> ApplicationCapability.ASSISTED_APPLY;
            default -> ApplicationCapability.MANUAL_REQUIRED;
        };
    }

    private boolean isSessionActive(String platformName) {
        try {
            return Boolean.TRUE.equals(platformConfigService.get(platformName).getSessionActive());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean supportsAuthorizedSubmission(AtsType source) {
        return switch (source) {
            case GREENHOUSE, WORKABLE -> true; // require an API key (not shipped)
            default -> false;
        };
    }
}
