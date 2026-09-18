package com.hellfire.controller;

import com.hellfire.model.User;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.payment.dto.CustomerBankAccountRequest;
import com.hellfire.payment.service.CustomerBankAccountService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The signed-in customer's saved refund account. */
@RestController
@RequestMapping("/api/users/me/bank-account")
@RequiredArgsConstructor
public class CustomerBankAccountController {

    private final CustomerBankAccountService service;
    private final UserService userService;

    /** 200 with the account, or 204 when none is saved. */
    @GetMapping
    public ResponseEntity<BankAccountDto> get(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return service.getOwn(user).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<BankAccountDto> upsert(@Valid @RequestBody CustomerBankAccountRequest request,
                                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(service.upsertOwn(user, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        service.deleteOwn(user);
        return ResponseEntity.noContent().build();
    }
}
