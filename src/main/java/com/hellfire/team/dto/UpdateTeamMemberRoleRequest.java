package com.hellfire.team.dto;

import com.hellfire.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTeamMemberRoleRequest {

    /** Must be one of the TEAM_* roles. */
    @NotNull
    private UserRole role;
}
