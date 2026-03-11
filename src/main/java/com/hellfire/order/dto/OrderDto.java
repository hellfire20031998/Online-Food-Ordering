package com.hellfire.order.dto;

import com.hellfire.model.PaymentMethods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private Long id;
    private Long customerId;
    private Long restaurantId;
    private Long totalAmount;
    private String orderStatus;
    private Date createdAt;
    private Long deliveryAddressId;
    private List<OrderItemDto> items;
    private Long totalItems;
    private Long totalPrice;
    private PaymentMethods paymentMethod;
}

