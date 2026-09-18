package com.hellfire.repository;

import com.hellfire.model.CustomerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerBankAccountRepository extends JpaRepository<CustomerBankAccount, Long> {

    Optional<CustomerBankAccount> findByUserId(Long userId);
}
