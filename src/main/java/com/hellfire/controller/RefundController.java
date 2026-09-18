package com.hellfire.controller;

import com.hellfire.model.User;
import com.hellfire.payment.dto.RefundDto;
import com.hellfire.payment.dto.RefundRequest;
import com.hellfire.payment.service.RefundService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Customers raise and follow refund requests on their own orders. */
@RestController
@RequestMapping("/api/orders/{orderId}/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RefundDto> request(@PathVariable Long orderId,
                                             @Valid @RequestBody RefundRequest request,
                                             @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return new ResponseEntity<>(refundService.request(orderId, user, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RefundDto>> list(@PathVariable Long orderId,
                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(refundService.forOrder(orderId, user));
    }
}
