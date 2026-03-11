package com.hellfire.cart.mapper;

import com.hellfire.cart.dto.CartDto;
import com.hellfire.cart.dto.CartItemDto;
import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;

import java.util.List;
import java.util.stream.Collectors;

public class CartMapper {

    public static CartDto toDto(Cart cart) {
        if (cart == null) {
            return null;
        }
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setCustomerId(cart.getCustomer() != null ? cart.getCustomer().getId() : null);
        dto.setTotal(cart.getTotal());
        dto.setItems(toItemDtos(cart.getItems()));
        return dto;
    }

    private static List<CartItemDto> toItemDtos(List<CartItem> items) {
        return items == null ? List.of() : items.stream().map(CartMapper::toItemDto).collect(Collectors.toList());
    }

    public static CartItemDto toItemDto(CartItem item) {
        if (item == null) {
            return null;
        }
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        if (item.getFood() != null) {
            dto.setFoodId(item.getFood().getId());
            dto.setFoodName(item.getFood().getName());
        }
        dto.setPrice(item.getTotalPrice());
        dto.setQuantity(item.getQuantity());
        return dto;
    }
}

