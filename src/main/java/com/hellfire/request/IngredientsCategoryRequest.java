package com.hellfire.request;


import lombok.Data;

@Data
public class IngredientsCategoryRequest {

    private String name;
    private Long restaurantId;
}
