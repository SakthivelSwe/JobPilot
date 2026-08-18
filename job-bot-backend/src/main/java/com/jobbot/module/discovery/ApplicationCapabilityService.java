package com.jobbot.module.discovery;

import org.springframework.stereotype.Service;

/**
 * Determines how a job may be applied to. India-market posture:
 *
 * <ul>
 *   <li>NAUKRI / INDEED → ASSISTED_APPLY: application-engine (local Playwright)
 *       may auto-apply within per-platform rate limits and user opt-in.</li>
 *   <li>LINKEDIN → MANUAL_REQUIRED: never server-side automated; the Chrome
 *       Extension handles Easy Apply inside the user's own real browser session.</li>
 *   <li>GREENHOUSE/ASHBY/LEVER/WORKABLE → ASSISTED_APPLY (pack prepared, user submits).</li>
 *   <li>Unknown / MANUAL → MANUAL_REQUIRED.</li>
 * </ul>
 */
@Service
public class ApplicationCapabilityService {

    /** Safe default classification based purely on the source type. */
    public ApplicationCapability determine(AtsType source) {
        return determine(source, false, false);
    }

    /**
     * Full determination.
     *
     * @param source                    the ATS/source
     * @param authorizedIntegration     an officially authorized submission API is configured
     * @param userApprovedAuto          the user explicitly enabled + approved auto submission
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

        // Authorized ATS integration path (still requires user opt-in).
        if (authorizedIntegration && userApprovedAuto && supportsAuthorizedSubmission(source)) {
            return ApplicationCapability.AUTO_ELIGIBLE;
        }

        return switch (source) {
            case NAUKRI, INDEED, GREENHOUSE, ASHBY, LEVER, WORKABLE
                    -> ApplicationCapability.ASSISTED_APPLY;
            default -> ApplicationCapability.MANUAL_REQUIRED;
        };
    }

    private boolean supportsAuthorizedSubmission(AtsType source) {
        return switch (source) {
            case GREENHOUSE, WORKABLE -> true; // require an API key (not shipped)
            default -> false;
        };
    }
}
