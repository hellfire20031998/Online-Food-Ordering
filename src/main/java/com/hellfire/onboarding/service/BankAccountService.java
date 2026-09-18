package com.hellfire.onboarding.service;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.Restaurant;
import com.hellfire.model.RestaurantBankAccount;
import com.hellfire.model.User;
import com.hellfire.onboarding.ApplicationMapper;
import com.hellfire.onboarding.BankDetails;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.onboarding.dto.BankAccountRequest;
import com.hellfire.repository.RestaurantBankAccountRepository;
import com.hellfire.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/** Payout account of a restaurant: readable/editable by its owner, readable by TEAM_ADMIN / TEAM_MANAGER. */
@Service
@RequiredArgsConstructor
public class BankAccountService {

    private static final Logger log = LoggerFactory.getLogger(BankAccountService.class);

    private final RestaurantBankAccountRepository bankAccountRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public BankAccountDto getForOwner(Long restaurantId, User owner) {
        requireOwner(restaurantId, owner);
        return ApplicationMapper.toDto(require(restaurantId), true);
    }

    @Transactional
    public BankAccountDto upsertForOwner(Long restaurantId, User owner, BankAccountRequest req) {
        Restaurant restaurant = requireOwner(restaurantId, owner);
        RestaurantBankAccount bank = bankAccountRepository.findByRestaurantId(restaurantId).orElseGet(() -> {
            RestaurantBankAccount created = new RestaurantBankAccount();
            created.setRestaurant(restaurant);
            return created;
        });
        bank.setAccountHolderName(req.getAccountHolderName().trim());
        bank.setAccountNumber(req.getAccountNumber().trim());
        bank.setIfsc(BankDetails.normalizeIfsc(req.getIfsc()));
        bank.setBankName(req.getBankName().trim());
        bank.setUpiId(BankDetails.normalizeUpi(req.getUpiId()));
        bank.setUpdatedAt(LocalDateTime.now());
        bank.setUpdatedBy(owner.getEmail());
        bankAccountRepository.save(bank);
        log.info("AUDIT bank account updated: restaurantId={} by {}", restaurantId, owner.getEmail());
        return ApplicationMapper.toDto(bank, true);
    }

    /** Team view; the controller restricts this to TEAM_ADMIN / TEAM_MANAGER. */
    @Transactional(readOnly = true)
    public BankAccountDto getForTeam(Long restaurantId) {
        return ApplicationMapper.toDto(require(restaurantId), true);
    }

    private RestaurantBankAccount require(Long restaurantId) {
        return bankAccountRepository.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("No payout account on file for restaurant " + restaurantId));
    }

    private Restaurant requireOwner(Long restaurantId, User user) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found"));
        boolean isOwner = restaurant.getOwner() != null && Objects.equals(restaurant.getOwner().getId(), user.getId());
        if (!isOwner) {
            throw new NotAuthorizedException("Only the restaurant owner can access the payout account");
        }
        return restaurant;
    }
}
