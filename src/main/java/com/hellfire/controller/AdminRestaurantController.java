package com.hellfire.controller;

import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.request.CreateRestaurantRequest;
import com.hellfire.response.MessageResponse;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/restaurants")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final RestaurantService restaurantService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Restaurant> createRestaurant(@Valid @RequestBody CreateRestaurantRequest req,
                                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        Restaurant restaurant = restaurantService.createRestaurant(req, user);
        return new ResponseEntity<>(restaurant, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Restaurant> updateRestaurant(@RequestBody CreateRestaurantRequest req,
                                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String jwt,
                                                       @PathVariable Long id) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        restaurantService.getRestaurantForUser(id, user);
        Restaurant restaurant = restaurantService.updateRestaurant(id, req);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteRestaurant(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwt,
                                                            @PathVariable Long id) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        restaurantService.getRestaurantForUser(id, user);
        restaurantService.deleteRestaurant(id);
        return new ResponseEntity<>(new MessageResponse("Restaurant deleted"), HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Restaurant> updateRestaurantStatus(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwt,
                                                             @PathVariable Long id) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        restaurantService.getRestaurantForUser(id, user);
        Restaurant restaurant = restaurantService.updateRestaurantStatus(id);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

    @GetMapping("/user")
    public ResponseEntity<Restaurant> findRestaurantByUserId(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        Restaurant restaurant = restaurantService.getRestaurantByUserId(user.getId());
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }
}
