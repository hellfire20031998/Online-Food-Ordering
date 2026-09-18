package com.hellfire.repository;

import com.hellfire.model.RestaurantBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantBankAccountRepository extends JpaRepository<RestaurantBankAccount, Long> {

    Optional<RestaurantBankAccount> findByRestaurantId(Long restaurantId);
}
