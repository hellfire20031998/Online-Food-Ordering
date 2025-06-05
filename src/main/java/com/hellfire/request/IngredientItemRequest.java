package com.hellfire.request;


import lombok.Data;

@Data
public class IngredientItemRequest {

    private String name;
    private Long restaurantId;
    private Long categoryId;

}
