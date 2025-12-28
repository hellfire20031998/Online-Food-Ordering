package com.hellfire.repository;

import com.hellfire.model.Restaurant;
import com.hellfire.model.RestaurantRole;
import com.hellfire.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRoleRepository extends JpaRepository<RestaurantRole, Long> {
    Optional<Object> findByUserAndRestaurant(User user, Restaurant restaurant);
}
