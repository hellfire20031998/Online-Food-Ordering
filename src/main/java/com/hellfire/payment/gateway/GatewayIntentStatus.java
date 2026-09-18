package com.hellfire.payment.gateway;

/** Provider-neutral state of a payment attempt. */
public record GatewayIntentStatus(State state, String failureReason) {

    public enum State {
        /** Not finished yet (awaiting the customer, processing, requires action). */
        PENDING,
        PAID,
        FAILED,
        CANCELLED
    }
}
