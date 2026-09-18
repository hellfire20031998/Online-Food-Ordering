package com.hellfire.model;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    /** Restaurant owner. */
    ADMIN,
    CUSTOMER,
    /** Restaurant staff, assigned by the owner. */
    MEMBER,
    MANAGER,

    /** Platform team: full control, including team member management. */
    TEAM_ADMIN,
    /** Platform team: operations across all restaurants and customers, payouts and refunds. */
    TEAM_MANAGER,
    /** Platform team: customer-facing support (read-only in phase 1). */
    TEAM_CUSTOMER_SUPPORT,
    /** Platform team: restaurant-facing support (read-only in phase 1). */
    TEAM_RESTAURANT_SUPPORT;

    public static final Set<UserRole> TEAM_ROLES =
            EnumSet.of(TEAM_ADMIN, TEAM_MANAGER, TEAM_CUSTOMER_SUPPORT, TEAM_RESTAURANT_SUPPORT);

    public boolean isTeamRole() {
        return TEAM_ROLES.contains(this);
    }
}
