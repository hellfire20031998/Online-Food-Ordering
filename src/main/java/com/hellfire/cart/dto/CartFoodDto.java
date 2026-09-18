package com.hellfire.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** The bits of a dish the cart UI needs, without exposing the full Food/Restaurant entities. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartFoodDto {

    private Long id;
    private String name;
    private BigDecimal price;
    private List<String> images;
    private Long restaurantId;
    private String restaurantName;
}
