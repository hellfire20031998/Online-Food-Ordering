package com.hellfire.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IngredientsCategoryRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long restaurantId;
}
