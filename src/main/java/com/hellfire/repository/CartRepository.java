package com.hellfire.repository;

import com.hellfire.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
public interface CartRepository extends JpaRepository<Cart,Long> {

    Cart findByCustomerId(Long customerId);
}
