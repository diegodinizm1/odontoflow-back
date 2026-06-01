package com.diego.odontoflowbackend.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID currentTenantId() {
        return UUID.fromString(
            (String) SecurityContextHolder.getContext().getAuthentication().getCredentials()
        );
    }

    public static UUID currentUserId() {
        return UUID.fromString(
            (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
