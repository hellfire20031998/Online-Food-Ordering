package com.hellfire.team.mapper;

import com.hellfire.model.*;
import com.hellfire.order.dto.OrderItemDto;
import com.hellfire.payment.PaymentMapper;
import com.hellfire.team.dto.TeamCustomerDto;
import com.hellfire.team.dto.TeamMemberDto;
import com.hellfire.team.dto.TeamOrderDto;
import com.hellfire.team.dto.TeamRestaurantDto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TeamMapper {

    private TeamMapper() {
    }

    public static UserStatus effectiveStatus(User user) {
        return user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
    }

    public static RestaurantStatus effectiveStatus(Restaurant restaurant) {
        return restaurant.getStatus() == null ? RestaurantStatus.ACTIVE : restaurant.getStatus();
    }

    public static TeamRestaurantDto toRestaurantDto(Restaurant r, long totalOrders) {
        TeamRestaurantDto dto = new TeamRestaurantDto();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setCuisineType(r.getCuisineType());
        dto.setCity(r.getAddress() != null ? r.getAddress().getCity() : null);
        dto.setDescription(r.getDescription());
        dto.setOpeningHours(r.getOpeningHours());
        dto.setImages(r.getImages());
        if (r.getOwner() != null) {
            dto.setOwnerId(r.getOwner().getId());
            dto.setOwnerName(r.getOwner().getFullName());
            dto.setOwnerEmail(r.getOwner().getEmail());
        }
        dto.setOpen(r.isOpen());
        dto.setStatus(effectiveStatus(r));
        dto.setRegistrationDate(r.getRegistrationDate());
        dto.setTotalOrders(totalOrders);
        return dto;
    }

    public static TeamCustomerDto toCustomerDto(User u, long totalOrders) {
        return new TeamCustomerDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                effectiveStatus(u), u.getCreatedAt(), totalOrders);
    }

    public static TeamMemberDto toMemberDto(User u) {
        return new TeamMemberDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                effectiveStatus(u), u.getCreatedAt());
    }

    public static TeamOrderDto toOrderDto(Order o) {
        TeamOrderDto dto = new TeamOrderDto();
        dto.setId(o.getId());
        if (o.getRestaurant() != null) {
            dto.setRestaurantId(o.getRestaurant().getId());
            dto.setRestaurantName(o.getRestaurant().getName());
        }
        if (o.getCustomer() != null) {
            dto.setCustomerId(o.getCustomer().getId());
            dto.setCustomerName(o.getCustomer().getFullName());
            dto.setCustomerEmail(o.getCustomer().getEmail());
        }
        dto.setOrderStatus(o.getOrderStatus());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setTotalItems(o.getTotalItems());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setDeliveryAddress(formatAddress(o.getDeliveryAddress()));
        dto.setItems(toItemDtos(o.getItems()));
        dto.setPayment(PaymentMapper.toDto(o.getPayment(), false));
        return dto;
    }

    private static String formatAddress(Address a) {
        if (a == null) {
            return null;
        }
        return Stream.of(a.getStreetAddress(), a.getCity(), a.getState(), a.getPincode(), a.getCountry())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static List<OrderItemDto> toItemDtos(List<OrderItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(item -> {
            OrderItemDto dto = new OrderItemDto();
            dto.setId(item.getId());
            if (item.getFood() != null) {
                dto.setFoodId(item.getFood().getId());
                dto.setFoodName(item.getFood().getName());
            }
            dto.setPrice(item.getTotalPrice());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList());
    }
}
