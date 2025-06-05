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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/food")
public class AdminFoodController {
    @Autowired
    private FoodService foodService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CategoryService categoryService;


    @PostMapping()
    public ResponseEntity<Food> createFood(@RequestBody CreateFoodRequest req,
                                           @RequestHeader("Authorization") String token) throws Exception {

        User user = userService.findUserByJwtToken(token);

        Restaurant restaurant = restaurantService.findRestaurantById(req.getRestaurantId());

//        Category category = categoryService.findCategoryById(req.getCategory());
        Food food = foodService.createFood(req,req.getCategory(),restaurant);

        return new ResponseEntity<>(food, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteFood(@PathVariable Long id,
                                           @RequestHeader("Authorization") String token) throws Exception {

        User user = userService.findUserByJwtToken(token);

        foodService.deleteFood(id);

        MessageResponse messageResponse = new MessageResponse("food deleted");

        return new ResponseEntity<>(messageResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Food> updateFoodAvailability(@PathVariable Long id,
                                                      @RequestHeader("Authorization") String token) throws Exception {

        User user = userService.findUserByJwtToken(token);

        Food food=foodService.updateAvailabilityStatus(id);

//        MessageResponse messageResponse = new MessageResponse("food deleted");

        return new ResponseEntity<>(food, HttpStatus.CREATED);
    }
}
