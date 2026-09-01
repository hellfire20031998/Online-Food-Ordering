package com.hellfire.controller;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.model.Order;
import com.hellfire.model.PaymentMethods;
import com.hellfire.model.User;
import com.hellfire.repository.OrderRepository;
import com.hellfire.request.ChangePaymentMethodRequest;
import com.hellfire.response.MessageResponse;
import com.hellfire.service.OrderService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping("/payment-methods")
    public ResponseEntity<PaymentMethods[]> getPaymentMethod() {
        return ResponseEntity.ok(PaymentMethods.values());
    }

    @PutMapping("/changeMethod")
    public ResponseEntity<MessageResponse> changePaymentMethod(
            @Valid @RequestBody ChangePaymentMethodRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Order order = orderService.findOrderById(request.getOrderId());

        boolean isCustomer = order.getCustomer() != null
                && Objects.equals(order.getCustomer().getId(), user.getId());
        boolean isRestaurantOwner = order.getRestaurant() != null
                && order.getRestaurant().getOwner() != null
                && Objects.equals(order.getRestaurant().getOwner().getId(), user.getId());
        if (!isCustomer && !isRestaurantOwner) {
            throw new NotAuthorizedException("You are not allowed to change this order's payment method");
        }

        order.setPaymentMethod(request.getPaymentMethod());
        orderRepository.save(order);
        return new ResponseEntity<>(new MessageResponse("Payment method updated"), HttpStatus.OK);
    }
}
