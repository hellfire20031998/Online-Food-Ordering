package com.hellfire.service;

import com.hellfire.model.Order;
import com.hellfire.model.User;
import com.hellfire.request.OrderRequest;

import java.util.List;

public interface   OrderService {


    Order createOrder(OrderRequest request, User user) throws Exception;

    Order updateOrder(Long orderId , String OrderStatus) throws Exception;

    void cancelOrder(Long orderId) throws Exception;

    List<Order> getUsersOrder(Long userId) throws Exception;

    List<Order> getRestaurantOrder(Long restaurantId,String status) throws Exception;

    Order findOrderById(Long orderId) throws Exception;


}
