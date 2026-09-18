package com.hellfire.model;

public enum RefundMethod {
    /** Reversed through the payment gateway that took the money. */
    GATEWAY,
    /** Cash-on-delivery orders: the team transfers to the customer's bank account. */
    BANK_TRANSFER
}
