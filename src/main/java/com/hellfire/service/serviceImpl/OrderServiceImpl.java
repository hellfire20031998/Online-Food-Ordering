package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.OrderStatusException;
import com.hellfire.model.*;
import com.hellfire.repository.*;
import com.hellfire.request.OrderRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.OrderService;
import com.hellfire.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RestaurantService restaurantService;
    @Autowired
    private CartService cartService;
    @Override
    public Order createOrder(OrderRequest request, User user) throws Exception {
       Address address = request.getDeliveryAddress();

       Address savedAddress = addressRepository.save(address);

       if(!user.getAddresses().contains(address)) {
           user.getAddresses().add(address);
           userRepository.save(user);
       }
       Restaurant restaurant = restaurantService.findRestaurantById(request.getRestaurantId());

       Order createdOrder = new Order();
       createdOrder.setRestaurant(restaurant);
       createdOrder.setCustomer(user);
       createdOrder.setDeliveryAddress(address);
       createdOrder.setCreatedAt(new Date());
       createdOrder.setOrderStatus("PENDING");
       createdOrder.setPaymentMethod(PaymentMethods.valueOf(request.getPaymentMethod()));

       Cart cart = cartService.findCartByUserId(user.getId());
       List<OrderItem> orderItems= new ArrayList<>();

       for(CartItem cartItem : cart.getItems()){
           OrderItem orderItem = new OrderItem();
           orderItem.setFood(cartItem.getFood());
           orderItem.setQuantity(cartItem.getQuantity());
//
           orderItem.setIngredients(new ArrayList<>(cartItem.getIngredients()));
           orderItem.setTotalPrice(cartItem.getTotalPrice());

           OrderItem savedOrderItem = orderItemRepository.save(orderItem);
           orderItems.add(savedOrderItem);
       }
       createdOrder.setItems(orderItems);
       createdOrder.setTotalAmount(createdOrder.getTotalPrice());
       createdOrder.setTotalItems((long)orderItems.size());
       createdOrder.setTotalPrice(cartService.calCartTotal(cart));


       Order savedOrder = orderRepository.save(createdOrder);

       restaurant.getOrders().add(savedOrder);

       return createdOrder;
    }

    @Override
    public Order updateOrder(Long orderId, String OrderStatus) throws Exception {
       Order order = findOrderById(orderId);

       if(OrderStatus.equals("PENDING")
               || OrderStatus.equals("OUT_FOR_DELIVERY")
               || OrderStatus.equals("DELIVERED")
               || OrderStatus.equals("COMPLETED")){

           order.setOrderStatus(OrderStatus);
           return orderRepository.save(order);

       }
       throw new OrderStatusException("Please choose a valid order status");

    }

    @Override
    public void cancelOrder(Long orderId) throws Exception {

        Order order=findOrderById(orderId);
        orderRepository.deleteById(orderId);

    }

    @Override
    public List<Order> getUsersOrder(Long userId) throws Exception {
        return orderRepository.findByCustomerId(userId);
    }

    @Override
    public List<Order> getRestaurantOrder(Long restaurantId, String status) throws Exception {
    List<Order> orders= orderRepository.findByRestaurantId(restaurantId);
    if(status!=null){
        orders=orders.stream().filter(order -> order.getOrderStatus().equals(status)).collect(Collectors.toList());
    }

    return orders;
    }

    @Override
    public Order findOrderById(Long orderId) throws Exception {
        Optional<Order> order=orderRepository.findById(orderId);
        if(order.isEmpty()){
            throw new Exception("Order not found");
        }
        return order.get();
    }
}
