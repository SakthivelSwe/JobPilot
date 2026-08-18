package com.jobbot.config;

import com.jobbot.security.SecurityUtils;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return SecurityUtils.getCurrentUserId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true; // Re-validate if the tenant changes
    }
}
