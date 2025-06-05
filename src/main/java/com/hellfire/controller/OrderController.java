package com.hellfire.controller;


import com.hellfire.model.Order;
import com.hellfire.model.USER_ROLE;
import com.hellfire.model.User;
import com.hellfire.request.OrderRequest;
import com.hellfire.response.Authorized;
import com.hellfire.response.OrderResponse;
import com.hellfire.service.OrderService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;


    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest,
                                         @RequestHeader("Authorization") String token) throws Exception {
        User user=userService.findUserByJwtToken(token);
        OrderResponse response=new OrderResponse();
//        if(!(user.getRole() == USER_ROLE.ADMIN || user.getRole() == USER_ROLE.MANAGER)){
//
//            response.setAuthorized(Authorized.UNAUTHORIZED);
//            return  new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//        }
        Order order = orderService.createOrder(orderRequest, user);
        response.setOrder(order);
        response.setAuthorized(Authorized.AUTHORIZED);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/order/user")
    public ResponseEntity<?> getOrderHistory(@RequestHeader("Authorization") String token) throws Exception {


        System.out.println("token: "+token);
        User user= userService.findUserByJwtToken(token);
//        System.out.println("User with jwt"+user);
        List<Order> orders = orderService.getUsersOrder(user.getId());

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    @DeleteMapping("/order/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id,
            @RequestHeader("Authorization") String token) throws Exception {
        User user=userService.findUserByJwtToken(token);
        OrderResponse response=new OrderResponse();
        if(!(user.getRole() == USER_ROLE.ADMIN )){

            response.setAuthorized(Authorized.UNAUTHORIZED);
            return  new ResponseEntity<>("UNSUCCESSFUL", HttpStatus.UNAUTHORIZED);
        }
        orderService.cancelOrder(id);
        return new ResponseEntity<>("SUCCESSFULLY", HttpStatus.OK);
    }
}
