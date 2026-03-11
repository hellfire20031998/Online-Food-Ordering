package com.hellfire.controller;

import com.hellfire.model.User;
import com.hellfire.order.dto.OrderDto;
import com.hellfire.order.mapper.OrderMapper;
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
    public ResponseEntity<List<OrderDto>> getOrderHistory(@PathVariable Long id,
                                             @RequestParam(required = false) String status,
                                             @RequestHeader("Authorization") String token) throws Exception {


        List<OrderDto> orders = OrderMapper.toDtos(orderService.getRestaurantOrder(id,status));
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PutMapping("/order/{id}/{status}")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long id,
                                               @PathVariable String status,
                                               @RequestHeader("Authorization") String token) throws Exception {
        return new ResponseEntity<>(OrderMapper.toDto(orderService.updateOrder(id,status)), HttpStatus.OK);
    }
}
