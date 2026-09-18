package com.hellfire.model;

public enum PaymentProvider {
    /** Cash collected by the restaurant on delivery; no gateway involved. */
    CASH_ON_DELIVERY,
    STRIPE,
    RAZORPAY;

    public boolean isGateway() {
        return this != CASH_ON_DELIVERY;
    }
}
