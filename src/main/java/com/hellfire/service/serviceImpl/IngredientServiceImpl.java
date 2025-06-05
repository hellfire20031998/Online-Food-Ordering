package com.hellfire.service.serviceImpl;


import com.hellfire.exceptions.IngredientCategoryIdNotFoundException;
import com.hellfire.model.Category;
import com.hellfire.model.IngredientsCategory;
import com.hellfire.model.IngredientsItem;
import com.hellfire.model.Restaurant;
import com.hellfire.repository.CategoryRepository;
import com.hellfire.repository.IngredientCategoryRepository;
import com.hellfire.repository.IngredientItemRepository;
import com.hellfire.service.IngredientsService;
import com.hellfire.service.RestaurantService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IngredientServiceImpl implements IngredientsService {

    @Autowired
    private IngredientCategoryRepository ingredientCategoryRepository;
    @Autowired
    private IngredientItemRepository ingredientItemRepository;
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private CategoryRepository categoryRepository;


    @Override
    public IngredientsCategory createIngredientsCategory(String categoryName, Long restaurantId) throws Exception {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);

        IngredientsCategory category = new IngredientsCategory();

        category.setName(categoryName);
        category.setRestaurant(restaurant);

        return ingredientCategoryRepository.save(category);
    }

    @Override
    public IngredientsCategory findIngredientCategoryById(Long id) throws Exception {

        Optional<IngredientsCategory> category = ingredientCategoryRepository.findById(id);

        if(category.isPresent()) {
            return category.get();
        }
        throw new IngredientCategoryIdNotFoundException("Ingredient with id " + id + " not found");
    }

    @Override
    public List<IngredientsCategory> findIngredientCategoryByRestaurantId(Long restaurantId) throws Exception {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        if(restaurant == null) {throw new IngredientCategoryIdNotFoundException("Ingredient Category By Restaurant with id " + restaurantId + " not found");}
      return ingredientCategoryRepository.findByRestaurantId(restaurantId);

    }

    @Override
    public IngredientsItem createIngredientsItem(String itemName, Long restaurantId, Long ingredientsCatId) throws Exception {
    Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
    IngredientsCategory category = findIngredientCategoryById(ingredientsCatId);
    IngredientsItem ingredientsItem = new IngredientsItem();
    ingredientsItem.setName(itemName);
    ingredientsItem.setRestaurant(restaurant);
    ingredientsItem.setCategory(category);

    category.getIngredients().add(ingredientsItem);

    return ingredientItemRepository.save(ingredientsItem);
    }

    @Override
    public List<IngredientsItem> findRestaurantIngredients(Long restaurantId) throws Exception {

        return ingredientItemRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public IngredientsItem updateStock(Long id) throws Exception {

        Optional<IngredientsItem> ingredientsItem = ingredientItemRepository.findById(id);
        if(ingredientsItem.isPresent()) {
            IngredientsItem ingredientsItem1 = ingredientsItem.get();
            ingredientsItem1.setInStock(!ingredientsItem1.isInStock());
            return ingredientItemRepository.save(ingredientsItem1);
        }
        throw new IngredientCategoryIdNotFoundException("Ingredient with id " + id + " not found");
    }
}
