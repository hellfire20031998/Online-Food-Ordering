package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.FoodIdNotFoundException;
import com.hellfire.model.Category;
import com.hellfire.model.Food;
import com.hellfire.model.Restaurant;
import com.hellfire.repository.FoodRepository;
import com.hellfire.request.CreateFoodRequest;
import com.hellfire.service.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodServiceImpl implements FoodService {

    @Autowired
    FoodRepository foodRepository;


    @Override
    public Food createFood(CreateFoodRequest req, Category category, Restaurant restaurant) {
        Food food = new Food();
        food.setFoodCategory(category);
        food.setRestaurant(restaurant);
        food.setDescription(req.getDescription());
        food.setPrice(req.getPrice());
        food.setImages(req.getImages());
        food.setName(req.getName());
        food.setIngredientsItems(req.getIngredients());
        food.setSeasonal(req.isSeasonal());
        food.setVegetarian(req.isVegetarian());
        food.setCreationDate(new Date());
        restaurant.getFoods().add(food);

        return foodRepository.save(food);

    }

    @Override
    public void deleteFood(Long foodId) throws Exception {
        Food food = foodRepository.findById(foodId).get();
        food.setRestaurant(null);
        foodRepository.delete(food);
    }

    @Override

    public List<Food> getRestaurantsFood(Long restaurantId,
                                         boolean isVegetarian,
                                         boolean isNonVegetarian,
                                         boolean isSeasonal,
                                         String foodCategory) {

        List<Food> foods = foodRepository.findByRestaurantId(restaurantId);


        // Apply filters only if the flag is true
        if (isVegetarian) {
            foods = filterByVegetarian(foods);
        }

        if (isNonVegetarian) {
            foods = filterByNonVeg(foods);
        }

        if (isSeasonal) {
            foods = filterByIsSeasonal(foods);
        }

//        if (foodCategory!=null && !foodCategory.isEmpty()) {
//            foods = filterByCategory(foods, foodCategory);
//        }
        if (foodCategory != null && !foodCategory.trim().isEmpty() && !foodCategory.equalsIgnoreCase("null")) {
            foods = filterByCategory(foods, foodCategory);
        }
        return foods;
    }


    private List<Food> filterByVegetarian(List<Food> foods) {
        return foods.stream()
                .filter(Food::isVegetarian)
                .collect(Collectors.toList());
    }

    private List<Food> filterByNonVeg(List<Food> foods) {
        return foods.stream()
                .filter(food -> !food.isVegetarian())
                .collect(Collectors.toList());
    }

    private List<Food> filterByIsSeasonal(List<Food> foods) {
        return foods.stream()
                .filter(Food::isSeasonal)
                .collect(Collectors.toList());
    }

    private List<Food> filterByCategory(List<Food> foods, String foodCategory) {
        return foods.stream()
                .filter(food -> food.getFoodCategory().getName().equalsIgnoreCase(foodCategory))
                .collect(Collectors.toList());
    }


    @Override
    public List<Food> searchFood(String foodCategory) {
        return foodRepository.searchFood(foodCategory);
    }

    @Override
    public Food findFoodById(Long foodId) throws Exception {
        Optional<Food> food = foodRepository.findById(foodId);
        if(food.isPresent()){
            return food.get();
        }
        throw new FoodIdNotFoundException("Food with id " + foodId + " not found");
    }

    @Override
    public Food updateAvailabilityStatus(Long foodId) throws Exception {
        Food food = findFoodById(foodId);
        food.setAvailable(!food.isAvailable());
        return foodRepository.save(food);
    }
}
