package com.hellfire.onboarding.controller;

import com.hellfire.model.User;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.onboarding.dto.BankAccountRequest;
import com.hellfire.onboarding.service.BankAccountService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Restaurant owner's own payout account. Staff roles (MANAGER/MEMBER) are deliberately excluded. */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/bank-account")
@RequiredArgsConstructor
public class OwnerBankAccountController {

    private final BankAccountService bankAccountService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<BankAccountDto> get(@PathVariable Long restaurantId,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(bankAccountService.getForOwner(restaurantId, user));
    }

    @PutMapping
    public ResponseEntity<BankAccountDto> upsert(@PathVariable Long restaurantId,
                                                 @Valid @RequestBody BankAccountRequest request,
                                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(bankAccountService.upsertForOwner(restaurantId, user, request));
    }
}
