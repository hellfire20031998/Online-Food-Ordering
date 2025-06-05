package com.hellfire.service;

import com.hellfire.model.IngredientsCategory;
import com.hellfire.model.IngredientsItem;

import java.util.List;
public interface IngredientsService {

    IngredientsCategory createIngredientsCategory(String categoryName, Long restaurantId) throws Exception;

    IngredientsCategory findIngredientCategoryById(Long id) throws Exception;

    List<IngredientsCategory> findIngredientCategoryByRestaurantId(Long restaurantId) throws Exception;

    IngredientsItem createIngredientsItem(String itemName, Long restaurantId, Long ingredientsCatId) throws Exception;

    List<IngredientsItem> findRestaurantIngredients(Long restaurantId) throws Exception;

    IngredientsItem updateStock(Long id) throws Exception;


}
