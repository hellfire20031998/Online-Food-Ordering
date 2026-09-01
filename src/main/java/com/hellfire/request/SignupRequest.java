package com.hellfire.request;

import com.hellfire.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "must be at least 6 characters")
    private String password;

    /**
     * Only CUSTOMER and ADMIN (restaurant owner) may be chosen at signup.
     * MANAGER/MEMBER are assigned by a restaurant owner via /api/restaurant-roles.
     */
    private UserRole role;
}
