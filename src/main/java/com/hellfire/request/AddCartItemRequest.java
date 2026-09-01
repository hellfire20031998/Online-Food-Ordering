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
}
