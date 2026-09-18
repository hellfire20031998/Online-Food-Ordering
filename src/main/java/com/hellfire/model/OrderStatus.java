package com.hellfire.model;

import com.hellfire.exceptions.OrderStatusException;

public enum OrderStatus {
    /** Online payment not yet confirmed; hidden from the restaurant. */
    PAYMENT_PENDING,
    /** Online payment declined or abandoned; the customer can retry or cancel. */
    PAYMENT_FAILED,
    PENDING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    public static OrderStatus fromString(String value) throws OrderStatusException {
        if (value == null || value.isBlank()) {
            throw new OrderStatusException("Please choose a valid order status");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OrderStatusException("Please choose a valid order status");
        }
    }

    /** Orders in these states can no longer be cancelled. */
    public boolean isFulfilled() {
        return this == DELIVERED || this == COMPLETED;
    }

    /** Set by the payment system, never by restaurant staff. */
    public boolean isPaymentState() {
        return this == PAYMENT_PENDING || this == PAYMENT_FAILED;
    }
}
