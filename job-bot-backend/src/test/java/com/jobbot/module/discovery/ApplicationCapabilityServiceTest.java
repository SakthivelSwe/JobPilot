package com.jobbot.module.discovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationCapabilityServiceTest {

    private final ApplicationCapabilityService service = new ApplicationCapabilityService();

    @Test
    void naukriAndIndeedAreAssistedApply() {
        // India platforms: application-engine auto-applies within rate limits.
        assertEquals(ApplicationCapability.ASSISTED_APPLY, service.determine(AtsType.NAUKRI));
        assertEquals(ApplicationCapability.ASSISTED_APPLY, service.determine(AtsType.INDEED));
    }

    @Test
    void publicAtsFeedsAreAssistedApplyByDefault() {
        assertEquals(ApplicationCapability.ASSISTED_APPLY, service.determine(AtsType.GREENHOUSE));
        assertEquals(ApplicationCapability.ASSISTED_APPLY, service.determine(AtsType.ASHBY));
    }

    @Test
    void linkedInIsAlwaysManualForServerSide() {
        // Chrome extension handles LinkedIn; server-side never auto-applies.
        assertEquals(ApplicationCapability.MANUAL_REQUIRED, service.determine(AtsType.LINKEDIN));
        assertEquals(ApplicationCapability.MANUAL_REQUIRED,
                service.determine(AtsType.LINKEDIN, true, true));
    }

    @Test
    void manualAndUnknownAreManual() {
        assertEquals(ApplicationCapability.MANUAL_REQUIRED, service.determine(AtsType.MANUAL));
        assertEquals(ApplicationCapability.MANUAL_REQUIRED, service.determine(AtsType.OTHER));
        assertEquals(ApplicationCapability.MANUAL_REQUIRED, service.determine(null));
    }

    @Test
    void autoOnlyWhenAllConditionsMet() {
        // Not auto unless authorized integration AND user approval AND supported source.
        assertEquals(ApplicationCapability.ASSISTED_APPLY,
                service.determine(AtsType.GREENHOUSE, false, true));
        assertEquals(ApplicationCapability.ASSISTED_APPLY,
                service.determine(AtsType.GREENHOUSE, true, false));
        assertEquals(ApplicationCapability.AUTO_ELIGIBLE,
                service.determine(AtsType.GREENHOUSE, true, true));
        // Ashby has no authorized-submission path → stays assisted even with flags.
        assertEquals(ApplicationCapability.ASSISTED_APPLY,
                service.determine(AtsType.ASHBY, true, true));
    }
}
