package com.hellfire.repository;

import com.hellfire.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    List<Order> findByCustomerId(Long customerId);

    List<Order> findByRestaurantId(Long restaurantId);
}
