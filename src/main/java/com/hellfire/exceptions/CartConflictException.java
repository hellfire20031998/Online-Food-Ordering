package com.hellfire.exceptions;

/** The cart already holds items from another restaurant. Mapped to HTTP 409. */
public class CartConflictException extends RuntimeException {

    private final String currentRestaurantName;

    public CartConflictException(String currentRestaurantName, String newRestaurantName) {
        super("Your cart has items from " + currentRestaurantName + ". Replace them to order from "
                + newRestaurantName + "?");
        this.currentRestaurantName = currentRestaurantName;
    }

    public String getCurrentRestaurantName() {
        return currentRestaurantName;
    }
}
