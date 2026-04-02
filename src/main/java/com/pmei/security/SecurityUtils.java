package com.pmei.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility class for accessing security context information.
 */
public class SecurityUtils {

    /**
     * Returns the authenticated user.
     */
    public static CustomUserPrincipal getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new IllegalStateException("User is not authenticated.");
        }

        return principal;
    }

    /**
     * Returns the authenticated user.
     */
    public static UUID getCompanyId() {
        return getAuthenticatedUser().getCompanyId();
    }

    /**
     * Returns authenticated user's email.
     */
    public static String getEmail() {
        return getAuthenticatedUser().getEmail();
    }

    /**
     * Returns authenticated user's email.
     */
    public static String getRole() {
        return getAuthenticatedUser().getAuthorities()
                .iterator().next().getAuthority();
    }
}

