package com.hellfire.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single-row table (id = 1) holding platform-wide settings editable from the team console.
 * The commission percentage here is the default applied to every restaurant; per-restaurant
 * overrides are a later phase.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "platform_settings")
public class PlatformSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionPercentage;

    @Column(length = 3, nullable = false)
    private String currency;

    private LocalDateTime updatedAt;

    private String updatedBy;
}
