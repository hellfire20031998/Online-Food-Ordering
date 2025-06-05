package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.CategoryNotFoundException;
import com.hellfire.model.Category;
import com.hellfire.model.Restaurant;
import com.hellfire.repository.CategoryRepository;
import com.hellfire.service.CategoryService;
import com.hellfire.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Override
    public Category createCategory(String name, Long userId) throws Exception {
        Restaurant restaurant =restaurantService.getRestaurantByUserId(userId);
        System.out.println("Category created"+name);
        Category category = new Category();

        category.setName(name);
        category.setRestaurant(restaurant);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> findCategoryByRestaurantId(Long restaurantId) throws Exception {

        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        return categoryRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public Category findCategoryById(Long id) throws Exception {
        Optional<Category> optionalCategory = categoryRepository.findById(id);

        if(optionalCategory.isPresent()){
            return optionalCategory.get();
        }

        throw new CategoryNotFoundException("Category not found");
    }
}
