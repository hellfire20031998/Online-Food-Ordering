package com.hellfire.controller;

import com.hellfire.model.Order;
import com.hellfire.model.User;
import com.hellfire.order.dto.OrderDto;
import com.hellfire.order.mapper.OrderMapper;
import com.hellfire.service.OrderService;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final RestaurantService restaurantService;

    @GetMapping("/order/restaurant/{id}")
    public ResponseEntity<List<OrderDto>> getOrderHistory(@PathVariable Long id,
                                                          @RequestParam(required = false) String status,
                                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        restaurantService.getRestaurantForUser(id, user);

        List<OrderDto> orders = OrderMapper.toDtos(orderService.getRestaurantOrder(id, status));
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PutMapping("/order/{id}/{status}")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long id,
                                                      @PathVariable String status,
                                                      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Order order = orderService.findOrderById(id);
        restaurantService.getRestaurantForUser(order.getRestaurant().getId(), user);

        return new ResponseEntity<>(OrderMapper.toDto(orderService.updateOrder(id, status)), HttpStatus.OK);
    }
}
