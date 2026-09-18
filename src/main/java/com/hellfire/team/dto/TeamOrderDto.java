package com.hellfire.team.dto;

import com.hellfire.model.OrderStatus;
import com.hellfire.model.PaymentMethods;
import com.hellfire.order.dto.OrderItemDto;
import com.hellfire.payment.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamOrderDto {

    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private OrderStatus orderStatus;
    private PaymentMethods paymentMethod;
    private BigDecimal totalAmount;
    private Long totalItems;
    private Date createdAt;
    private String deliveryAddress;
    private List<OrderItemDto> items;
    private PaymentDto payment;
}
