package com.hellfire.controller;

import com.hellfire.dto.RestaurantDto;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> searchRestaurant(@RequestParam String name) {
        List<Restaurant> restaurants = restaurantService.searchRestaurant(name);
        return new ResponseEntity<>(restaurants, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurant() {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        return new ResponseEntity<>(restaurants, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> findRestaurantById(@PathVariable Long id) throws Exception {
        Restaurant restaurant = restaurantService.findRestaurantById(id);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

    @PutMapping("/{id}/add-favorites")
    public ResponseEntity<RestaurantDto> addToFavorites(@RequestHeader(HttpHeaders.AUTHORIZATION) String token,
                                                        @PathVariable Long id) throws Exception {
        User user = userService.findUserByJwtToken(token);
        RestaurantDto restaurant = restaurantService.addToFavourites(id, user);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }
}
