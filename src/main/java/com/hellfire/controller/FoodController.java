package com.hellfire.controller;


import com.hellfire.model.Food;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.request.CreateFoodRequest;
import com.hellfire.service.FoodService;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestaurantService restaurantService;


    @GetMapping("/search")
    public ResponseEntity<List<Food>> searchFood(@RequestParam String name,
                                           @RequestHeader("Authorization") String token) throws Exception {

        User user = userService.findUserByJwtToken(token);


        List<Food> foods = foodService.searchFood(name);

        return new ResponseEntity<>(foods, HttpStatus.CREATED);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Food>> getRestaurantFood(@RequestParam(required = false) boolean vegetarian,
                                                 @RequestParam(required = false) boolean seasonal,
                                                 @RequestParam(required = false) boolean nonVegetarian,
                                                 @PathVariable long restaurantId,
                                                 @RequestParam(required = false) String foodCategory,
                                                 @RequestHeader("Authorization") String token) throws Exception {

        User user = userService.findUserByJwtToken(token);
        List<Food> foods = foodService.getRestaurantsFood(restaurantId,vegetarian,nonVegetarian,seasonal,foodCategory);
        return new ResponseEntity<>(foods, HttpStatus.OK);
    }

}
