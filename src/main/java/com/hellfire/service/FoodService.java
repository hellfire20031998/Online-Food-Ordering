package com.hellfire.service;


import com.hellfire.model.Category;
import com.hellfire.model.Food;
import com.hellfire.model.Restaurant;
import com.hellfire.request.CreateFoodRequest;

import java.util.List;


public interface FoodService {


    Food createFood(CreateFoodRequest req, Category category, Restaurant restaurant);

    void deleteFood(Long foodId) throws Exception;

    List<Food> getRestaurantsFood(Long restaurantId,
                                  boolean isVegetarian,
                                  boolean isNonVegetarian,
                                  boolean isSeasonal,
                                  String foodCategory);

    List<Food> searchFood(String foodCategory);

    Food findFoodById(Long foodId) throws Exception;

    Food updateAvailabilityStatus(Long foodId) throws Exception ;
}
