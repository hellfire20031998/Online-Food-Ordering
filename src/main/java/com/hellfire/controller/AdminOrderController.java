package com.hellfire.controller;

import com.hellfire.model.Order;
import com.hellfire.model.User;
import com.hellfire.request.OrderRequest;
import com.hellfire.service.OrderService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;




    @GetMapping("/order/restaurant/{id}")
    public ResponseEntity<?> getOrderHistory(@PathVariable Long id,
                                             @RequestParam(required = false) String status,
                                             @RequestHeader("Authorization") String token) throws Exception {


        List<Order> orders = orderService.getRestaurantOrder(id,status);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PutMapping("/order/{id}/{status}")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                               @PathVariable String status,
                                               @RequestHeader("Authorization") String token) throws Exception {
        Order order = orderService.updateOrder(id,status);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }
}
