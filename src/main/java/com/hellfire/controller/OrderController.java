package com.hellfire.controller;

import com.hellfire.model.Order;
import com.hellfire.model.User;
import com.hellfire.order.dto.OrderDto;
import com.hellfire.order.mapper.OrderMapper;
import com.hellfire.request.OrderRequest;
import com.hellfire.service.OrderService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping("/order")
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody OrderRequest orderRequest,
                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Order order = orderService.createOrder(orderRequest, user);
        // The paying customer gets the client secret so the browser can complete an online payment.
        return new ResponseEntity<>(OrderMapper.toDto(order, true), HttpStatus.CREATED);
    }

    @GetMapping("/order/user")
    public ResponseEntity<List<OrderDto>> getOrderHistory(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        List<OrderDto> orders = orderService.getUsersOrder(user.getId()).stream()
                .map(order -> OrderMapper.toDto(order, true))
                .toList();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @DeleteMapping("/order/{id}")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long id,
                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Order cancelled = orderService.cancelOrder(id, user);
        return new ResponseEntity<>(OrderMapper.toDto(cancelled), HttpStatus.OK);
    }
}
