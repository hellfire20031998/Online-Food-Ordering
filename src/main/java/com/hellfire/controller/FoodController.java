package com.hellfire.controller;

import com.hellfire.model.Food;
import com.hellfire.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/search")
    public ResponseEntity<List<Food>> searchFood(@RequestParam String name) {
        List<Food> foods = foodService.searchFood(name);
        return new ResponseEntity<>(foods, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Food>> getRestaurantFood(@RequestParam(required = false) boolean vegetarian,
                                                        @RequestParam(required = false) boolean seasonal,
                                                        @RequestParam(required = false) boolean nonVegetarian,
                                                        @PathVariable long restaurantId,
                                                        @RequestParam(required = false) String foodCategory) {
        List<Food> foods = foodService.getRestaurantsFood(restaurantId, vegetarian, nonVegetarian, seasonal, foodCategory);
        return new ResponseEntity<>(foods, HttpStatus.OK);
    }
}
