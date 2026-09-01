package com.hellfire.request;

import com.hellfire.model.Category;
import com.hellfire.model.IngredientsItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateFoodRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    private Category category;

    private List<String> images;

    @NotNull
    private Long restaurantId;

    private boolean vegetarian;

    private boolean seasonal;

    private List<IngredientsItem> ingredients;
}
