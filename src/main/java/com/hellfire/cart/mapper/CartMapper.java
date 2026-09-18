package com.hellfire.cart.mapper;

import com.hellfire.cart.dto.CartDto;
import com.hellfire.cart.dto.CartFoodDto;
import com.hellfire.cart.dto.CartItemDto;
import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.Food;

import java.util.ArrayList;
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
        cart.getItems().stream()
                .map(CartItem::getFood)
                .filter(f -> f != null && f.getRestaurant() != null)
                .findFirst()
                .ifPresent(f -> {
                    dto.setRestaurantId(f.getRestaurant().getId());
                    dto.setRestaurantName(f.getRestaurant().getName());
                });
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
        Food food = item.getFood();
        if (food != null) {
            dto.setFoodId(food.getId());
            dto.setFoodName(food.getName());
            dto.setFood(new CartFoodDto(
                    food.getId(),
                    food.getName(),
                    food.getPrice(),
                    food.getImages() == null ? List.of() : new ArrayList<>(food.getImages()),
                    food.getRestaurant() != null ? food.getRestaurant().getId() : null,
                    food.getRestaurant() != null ? food.getRestaurant().getName() : null));
        }
        dto.setPrice(item.getTotalPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setQuantity(item.getQuantity());
        dto.setIngredients(item.getIngredients() == null ? List.of() : new ArrayList<>(item.getIngredients()));
        return dto;
    }
}
