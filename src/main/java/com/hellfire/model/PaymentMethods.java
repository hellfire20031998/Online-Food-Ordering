package com.hellfire.model;

public enum PaymentMethods {
    CASH_ON_DELIVERY,
    CREDIT_CARD,
    DEBIT_CARD,
    NET_BANKING,
    UPI,
    WALLET;

    public static PaymentMethods fromString(String value) {
        try {
            return PaymentMethods.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payment method: " + value);
        }
    }
}
