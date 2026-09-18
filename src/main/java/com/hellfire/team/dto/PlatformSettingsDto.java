package com.hellfire.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsDto {

    private BigDecimal commissionPercentage;
    private String currency;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
