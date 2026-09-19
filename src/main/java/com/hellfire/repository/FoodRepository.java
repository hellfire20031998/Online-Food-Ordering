package com.hellfire.repository;

import com.hellfire.model.Food;
import com.hellfire.model.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findByRestaurantId(Long restaurantId);

    /**
     * Customer-facing dish search: case-insensitive match on the dish name, description or category,
     * limited to dishes that are available and belong to a restaurant customers can see.
     */
    @Query("SELECT f FROM Food f JOIN f.restaurant r LEFT JOIN f.foodCategory c " +
            "WHERE f.available = true AND (r.status IS NULL OR r.status <> :suspended) AND (" +
            "lower(f.name) LIKE lower(concat('%', :query, '%')) " +
            "OR lower(f.description) LIKE lower(concat('%', :query, '%')) " +
            "OR lower(c.name) LIKE lower(concat('%', :query, '%'))) " +
            "ORDER BY r.open DESC, lower(f.name) ASC")
    List<Food> searchFood(@Param("query") String query, @Param("suspended") RestaurantStatus suspended);

}
