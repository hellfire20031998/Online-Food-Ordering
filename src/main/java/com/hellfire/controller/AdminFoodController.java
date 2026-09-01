package com.hellfire.controller;

import com.hellfire.model.Category;
import com.hellfire.model.Food;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.request.CreateFoodRequest;
import com.hellfire.response.MessageResponse;
import com.hellfire.service.CategoryService;
import com.hellfire.service.FoodService;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/food")
@RequiredArgsConstructor
public class AdminFoodController {

    private final FoodService foodService;
    private final UserService userService;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<Food> createFood(@Valid @RequestBody CreateFoodRequest req,
                                           @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Restaurant restaurant = restaurantService.getRestaurantForUser(req.getRestaurantId(), user);

        Category category = req.getCategory();
        if (category != null && category.getId() != null) {
            category = categoryService.findCategoryById(category.getId());
        }

        Food food = foodService.createFood(req, category, restaurant);
        return new ResponseEntity<>(food, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteFood(@PathVariable Long id,
                                                      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Food food = foodService.findFoodById(id);
        restaurantService.getRestaurantForUser(food.getRestaurant().getId(), user);

        foodService.deleteFood(id);
        return new ResponseEntity<>(new MessageResponse("Food deleted"), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Food> updateFoodAvailability(@PathVariable Long id,
                                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Food food = foodService.findFoodById(id);
        restaurantService.getRestaurantForUser(food.getRestaurant().getId(), user);

        Food updated = foodService.updateAvailabilityStatus(id);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }
}
