package com.hellfire.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IngredientItemRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long restaurantId;

    @NotNull
    private Long categoryId;
}
