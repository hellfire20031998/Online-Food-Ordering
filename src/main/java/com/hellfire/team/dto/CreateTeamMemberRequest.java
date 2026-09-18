package com.hellfire.team.dto;

import com.hellfire.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTeamMemberRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "must be at least 6 characters")
    private String password;

    /** Must be one of the TEAM_* roles. */
    @NotNull
    private UserRole role;
}
