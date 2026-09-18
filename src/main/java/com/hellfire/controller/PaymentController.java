package com.hellfire.controller;

import com.hellfire.model.PaymentMethods;
import com.hellfire.model.User;
import com.hellfire.payment.dto.PaymentConfigDto;
import com.hellfire.payment.dto.PaymentDto;
import com.hellfire.payment.service.PaymentService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Customer-facing payment endpoints plus the public gateway webhook. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping("/payment-methods")
    public ResponseEntity<PaymentMethods[]> getPaymentMethod() {
        return ResponseEntity.ok(PaymentMethods.values());
    }

    /** Tells the checkout page whether online payment is available and which browser key to use. */
    @GetMapping("/payments/config")
    public ResponseEntity<PaymentConfigDto> config() {
        return ResponseEntity.ok(paymentService.config());
    }

    /** The customer's own payment for an order, with the client secret while it is still payable. */
    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<PaymentDto> forOrder(@PathVariable Long orderId,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(paymentService.getForOrder(orderId, user));
    }

    /** Called by the browser after the gateway reports success; verifies with the gateway server-side. */
    @PostMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentDto> confirm(@PathVariable Long paymentId,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(paymentService.confirm(paymentId, user));
    }

    /** Public: gateway webhook. Authenticity is proven by the signature, not a JWT. */
    @PostMapping("/payments/webhook/stripe")
    public ResponseEntity<Void> stripeWebhook(@RequestBody String payload,
                                              @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
