package com.hellfire.repository;


import com.hellfire.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Restaurant findByOwnerId(long id);

    @Query("SELECT r from Restaurant  r where  lower(r.name) LIKE lower(concat('%',:query,'%')) or lower(r.cuisineType) like lower(concat('%',:query,'%') )")
    List<Restaurant> findBySearchQuery(String query);
}
