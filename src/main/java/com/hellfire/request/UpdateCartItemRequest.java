package com.hellfire.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartItemRequest {

    @NotNull
    private Long cartItemId;

    @Min(1)
    private int quantity;
}
