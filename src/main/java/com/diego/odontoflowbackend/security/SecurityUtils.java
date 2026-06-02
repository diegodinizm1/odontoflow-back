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

    /** Current user's role (e.g. "DENTIST", "RECEPTIONIST"), without the "ROLE_" prefix. */
    public static String currentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }
}
