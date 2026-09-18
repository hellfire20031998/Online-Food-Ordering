package com.hellfire.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDto {

    private Long id;
    private Long foodId;
    private String foodName;
    /** Line total (kept for older clients; same as totalPrice). */
    private BigDecimal price;
    private BigDecimal totalPrice;
    private int quantity;
    private List<String> ingredients;
    private CartFoodDto food;
}
