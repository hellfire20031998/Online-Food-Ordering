package com.hellfire.team.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePlatformSettingsRequest {

    @NotNull
    @DecimalMin(value = "0.00", message = "must be at least 0")
    @DecimalMax(value = "100.00", message = "must be at most 100")
    private BigDecimal commissionPercentage;
}
