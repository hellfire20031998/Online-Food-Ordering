package com.hellfire.service;

import com.hellfire.Dto.RestaurantDto;
import com.hellfire.model.Restaurant;
import com.hellfire.model.RestaurantRole;
import com.hellfire.model.User;
import com.hellfire.request.CreateRestaurantRequest;

import java.util.List;

public interface RestaurantService {


    Restaurant createRestaurant(CreateRestaurantRequest req, User user);

    Restaurant updateRestaurant(Long id ,CreateRestaurantRequest updateRequest) throws Exception;

    void deleteRestaurant(Long id)throws Exception;

    List<Restaurant> getAllRestaurants();

    List<Restaurant> searchRestaurant(String query);

    Restaurant findRestaurantById(Long id) throws Exception;

    Restaurant getRestaurantByUserId(Long id) throws Exception;

    RestaurantDto addToFavourites(Long id,User user) throws Exception;

    Restaurant updateRestaurantStatus(Long id) throws Exception;

}
