package com.hellfire.model;

/**
 * Platform-level status set by the team. Independent of the owner-controlled
 * {@code open} flag: a SUSPENDED restaurant is hidden from customers and cannot receive orders.
 */
public enum RestaurantStatus {
    ACTIVE,
    SUSPENDED
}
