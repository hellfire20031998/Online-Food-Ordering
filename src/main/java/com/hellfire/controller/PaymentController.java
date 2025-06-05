package com.hellfire.controller;

import com.hellfire.model.Order;
import com.hellfire.model.PaymentMethods;
import com.hellfire.model.USER_ROLE;
import com.hellfire.model.User;
import com.hellfire.repository.OrderRepository;
import com.hellfire.request.ChangePaymentMethodRequest;
import com.hellfire.service.OrderService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {
    @Autowired
    UserService userService;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    @GetMapping("/payment-methods")
    public ResponseEntity<?> getPaymentMethod() {
        PaymentMethods[] methods = PaymentMethods.values();
        return ResponseEntity.ok(methods);
    }
    @PutMapping("/changeMethod")
    public ResponseEntity<?> changePaymentMethod(@RequestBody ChangePaymentMethodRequest request,
                                                 @RequestHeader("Authorization") String token) {
        try {
            User user = userService.findUserByJwtToken(token);
            if (user == null || user.getRole() != USER_ROLE.ADMIN) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            Order order = orderService.findOrderById(request.getOrderId()); // Implement this in your service
            if (order == null) {
                return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
            }

            order.setPaymentMethod(request.getPaymentMethod());

            orderRepository.save(order);
            return new ResponseEntity<>("Payment method updated", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
