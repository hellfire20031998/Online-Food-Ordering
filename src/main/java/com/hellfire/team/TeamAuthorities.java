package com.hellfire.team;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

/**
 * SpEL fragments for {@code @PreAuthorize} on team-console endpoints.
 * Route-level access to /api/team/** is granted to every team role in AppConfig;
 * these tighten individual actions.
 */
public final class TeamAuthorities {

    public static final String ADMIN_ONLY = "hasAuthority('TEAM_ADMIN')";
    public static final String ADMIN_OR_MANAGER = "hasAnyAuthority('TEAM_ADMIN','TEAM_MANAGER')";
    /** Approving / rejecting restaurant applications. */
    public static final String RESTAURANT_ONBOARDING =
            "hasAnyAuthority('TEAM_ADMIN','TEAM_MANAGER','TEAM_RESTAURANT_SUPPORT')";

    private static final Set<String> BANK_REVEAL = Set.of("TEAM_ADMIN", "TEAM_MANAGER");

    private TeamAuthorities() {
    }

    /** Whether the caller may see unmasked bank account numbers. */
    public static boolean canRevealBank(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(BANK_REVEAL::contains);
    }
}
