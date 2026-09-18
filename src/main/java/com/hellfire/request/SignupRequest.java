package com.hellfire.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Public signup always creates a CUSTOMER. Restaurant owners are onboarded through
 * the team-approved application flow; platform team members are created by a TEAM_ADMIN.
 * Any "role" field sent by older clients is ignored.
 */
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
}
