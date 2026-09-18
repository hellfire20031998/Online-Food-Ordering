package com.hellfire.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopRestaurantDto {

    private Long restaurantId;
    private String name;
    private long orderCount;
    private BigDecimal revenue;
}
