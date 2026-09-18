package com.hellfire.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddCartItemRequest {

    @NotNull
    private Long foodId;

    @Min(1)
    private int quantity;

    private List<String> ingredients;

    /**
     * A cart holds items from one restaurant. When this dish belongs to a different restaurant the
     * request is refused with 409 unless {@code replaceCart} is true, in which case the cart is
     * emptied first.
     */
    private boolean replaceCart;
}
