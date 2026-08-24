package com.jobbot.module.platform;

import java.time.OffsetDateTime;

/**
 * Safe DTO returned to the frontend for session status.
 * The sessionFilePath (sensitive local path) is deliberately excluded.
 */
public record PlatformSessionDTO(
        String platformName,
        /** DISCONNECTED | CONNECTED | EXPIRED | ERROR */
        String sessionStatus,
        boolean sessionActive,
        /** Display name/email of linked account. Never used for auth. */
        String sessionUsername,
        OffsetDateTime sessionConnectedAt
) {}
