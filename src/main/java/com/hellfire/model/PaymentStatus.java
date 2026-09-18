package com.hellfire.model;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED;

    /** Money has been received (possibly partly returned since). */
    public boolean isSettled() {
        return this == PAID || this == PARTIALLY_REFUNDED || this == REFUNDED;
    }
}
