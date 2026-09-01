package com.hellfire.request;

import com.hellfire.model.UserRole;
import lombok.Data;

@Data
public class UserAndRoleRequest {

    private Long userId;
    private String role;
    private Long restaurantId;

    public UserRole getEnumRole() {
        try {
            return role == null ? null : UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role value: " + role);
        }
    }
}
