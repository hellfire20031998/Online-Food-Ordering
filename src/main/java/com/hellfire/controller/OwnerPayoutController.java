package com.hellfire.controller;

import com.hellfire.model.User;
import com.hellfire.payment.dto.EarningsSummaryDto;
import com.hellfire.payment.dto.PayoutDto;
import com.hellfire.payment.service.PayoutService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Restaurant owner's view of earnings and payouts. Owner only (staff roles excluded). */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}")
@RequiredArgsConstructor
public class OwnerPayoutController {

    private final PayoutService payoutService;
    private final UserService userService;

    @GetMapping("/earnings")
    public ResponseEntity<EarningsSummaryDto> earnings(@PathVariable Long restaurantId,
                                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(payoutService.earnings(restaurantId, user));
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutDto>> payouts(@PathVariable Long restaurantId,
                                                   @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(payoutService.listForOwner(restaurantId, user));
    }

    @GetMapping("/payouts/{payoutId}")
    public ResponseEntity<PayoutDto> payout(@PathVariable Long restaurantId,
                                            @PathVariable Long payoutId,
                                            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(payoutService.getForOwner(restaurantId, payoutId, user));
    }
}
