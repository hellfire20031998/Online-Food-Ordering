package com.hellfire.service;

import com.hellfire.dto.RestaurantDto;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.request.CreateRestaurantRequest;

import java.util.List;

public interface RestaurantService {

    Restaurant createRestaurant(CreateRestaurantRequest req, User user);

    Restaurant updateRestaurant(Long id, CreateRestaurantRequest updateRequest) throws Exception;

    void deleteRestaurant(Long id) throws Exception;

    /** Customer-facing listing: suspended restaurants are excluded. */
    List<Restaurant> getAllRestaurants();

    /** Customer-facing search: suspended restaurants are excluded. */
    List<Restaurant> searchRestaurant(String query);

    /** Internal lookup by id regardless of platform status. */
    Restaurant findRestaurantById(Long id) throws Exception;

    /** Customer-facing lookup: a suspended restaurant is reported as not found. */
    Restaurant getPublicRestaurant(Long id) throws Exception;

    Restaurant getRestaurantByUserId(Long id) throws Exception;

    /**
     * Loads the restaurant and verifies the given user may manage it
     * (owner, or assigned a staff role for it). Throws NotAuthorizedException otherwise.
     */
    Restaurant getRestaurantForUser(Long restaurantId, User user) throws Exception;

    RestaurantDto addToFavourites(Long id, User user) throws Exception;

    Restaurant updateRestaurantStatus(Long id) throws Exception;
}
