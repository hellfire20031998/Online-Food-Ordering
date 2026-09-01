package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.CategoryNotFoundException;
import com.hellfire.model.Category;
import com.hellfire.model.Restaurant;
import com.hellfire.repository.CategoryRepository;
import com.hellfire.service.CategoryService;
import com.hellfire.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final RestaurantService restaurantService;

    @Override
    @Transactional
    public Category createCategory(String name, Long userId) throws Exception {
        Restaurant restaurant = restaurantService.getRestaurantByUserId(userId);

        Category category = new Category();
        category.setName(name);
        category.setRestaurant(restaurant);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> findCategoryByRestaurantId(Long restaurantId) throws Exception {
        restaurantService.findRestaurantById(restaurantId);
        return categoryRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public Category findCategoryById(Long id) throws Exception {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }
}
