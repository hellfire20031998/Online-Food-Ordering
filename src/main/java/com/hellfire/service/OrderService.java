package com.hellfire.service;

import com.hellfire.model.Order;
import com.hellfire.model.User;
import com.hellfire.request.OrderRequest;

import java.util.List;

public interface OrderService {

    Order createOrder(OrderRequest request, User user) throws Exception;

    Order updateOrder(Long orderId, String orderStatus) throws Exception;

    Order cancelOrder(Long orderId, User user) throws Exception;

    List<Order> getUsersOrder(Long userId);

    List<Order> getRestaurantOrder(Long restaurantId, String status);

    Order findOrderById(Long orderId) throws Exception;
}
