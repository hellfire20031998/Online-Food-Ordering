package com.hellfire.order.mapper;

import com.hellfire.cart.dto.CartItemDto;
import com.hellfire.model.Order;
import com.hellfire.model.OrderItem;
import com.hellfire.order.dto.OrderDto;
import com.hellfire.order.dto.OrderItemDto;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomer() != null ? order.getCustomer().getId() : null);
        dto.setRestaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null);
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setDeliveryAddressId(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getId() : null);
        dto.setItems(toItemDtos(order.getItems()));
        dto.setTotalItems(order.getTotalItems());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setPaymentMethod(order.getPaymentMethod());
        return dto;
    }

    public static List<OrderDto> toDtos(List<Order> orders) {
        return orders == null ? List.of() : orders.stream().map(OrderMapper::toDto).collect(Collectors.toList());
    }

    private static List<OrderItemDto> toItemDtos(List<OrderItem> items) {
        return items == null ? List.of() : items.stream().map(OrderMapper::toItemDto).collect(Collectors.toList());
    }

    private static OrderItemDto toItemDto(OrderItem item) {
        if (item == null) {
            return null;
        }
        OrderItemDto dto = new OrderItemDto();
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

