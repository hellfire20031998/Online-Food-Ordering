package com.hellfire.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectApplicationRequest {

    @NotBlank(message = "a reason is required")
    @Size(max = 1000)
    private String reason;
}
