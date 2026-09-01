package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.IngredientCategoryIdNotFoundException;
import com.hellfire.model.IngredientsCategory;
import com.hellfire.model.IngredientsItem;
import com.hellfire.model.Restaurant;
import com.hellfire.repository.IngredientCategoryRepository;
import com.hellfire.repository.IngredientItemRepository;
import com.hellfire.service.IngredientsService;
import com.hellfire.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientsService {

    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final IngredientItemRepository ingredientItemRepository;
    private final RestaurantService restaurantService;

    @Override
    @Transactional
    public IngredientsCategory createIngredientsCategory(String categoryName, Long restaurantId) throws Exception {
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);

        IngredientsCategory category = new IngredientsCategory();
        category.setName(categoryName);
        category.setRestaurant(restaurant);
        return ingredientCategoryRepository.save(category);
    }

    @Override
    public IngredientsCategory findIngredientCategoryById(Long id) throws Exception {
        return ingredientCategoryRepository.findById(id)
                .orElseThrow(() -> new IngredientCategoryIdNotFoundException("Ingredient category with id " + id + " not found"));
    }

    @Override
    public List<IngredientsCategory> findIngredientCategoryByRestaurantId(Long restaurantId) throws Exception {
        restaurantService.findRestaurantById(restaurantId);
        return ingredientCategoryRepository.findByRestaurantId(restaurantId);
    }

    @Override
    @Transactional
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
    @Transactional
    public IngredientsItem updateStock(Long id) throws Exception {
        IngredientsItem item = ingredientItemRepository.findById(id)
                .orElseThrow(() -> new IngredientCategoryIdNotFoundException("Ingredient with id " + id + " not found"));
        item.setInStock(!item.isInStock());
        return ingredientItemRepository.save(item);
    }
}
