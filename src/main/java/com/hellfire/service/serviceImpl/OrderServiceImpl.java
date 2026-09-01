package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.OrderNotFoundException;
import com.hellfire.exceptions.OrderStatusException;
import com.hellfire.model.*;
import com.hellfire.repository.AddressRepository;
import com.hellfire.repository.OrderItemRepository;
import com.hellfire.repository.OrderRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.OrderRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.OrderService;
import com.hellfire.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final Set<String> VALID_STATUSES = Set.of(
            STATUS_PENDING, STATUS_OUT_FOR_DELIVERY, STATUS_DELIVERED, STATUS_COMPLETED, STATUS_CANCELLED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final RestaurantService restaurantService;
    private final CartService cartService;

    @Override
    @Transactional
    public Order createOrder(OrderRequest request, User user) throws Exception {
        PaymentMethods paymentMethod = PaymentMethods.fromString(request.getPaymentMethod());
        Restaurant restaurant = restaurantService.findRestaurantById(request.getRestaurantId());

        Cart cart = cartService.findCartByUserId(user.getId());
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart");
        }

        Address address = resolveDeliveryAddress(request.getDeliveryAddress(), user);

        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCustomer(user);
        order.setDeliveryAddress(address);
        order.setCreatedAt(new Date());
        order.setOrderStatus(STATUS_PENDING);
        order.setPaymentMethod(paymentMethod);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setFood(cartItem.getFood());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setIngredients(cartItem.getIngredients() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(cartItem.getIngredients()));
            orderItem.setTotalPrice(cartItem.getTotalPrice());
            orderItems.add(orderItemRepository.save(orderItem));
        }

        order.setItems(orderItems);
        order.setTotalItems((long) orderItems.size());
        order.setTotalPrice(cartService.calCartTotal(cart));
        order.setTotalAmount(order.getTotalPrice());

        Order savedOrder = orderRepository.save(order);

        cartService.clearCart(user.getId());

        return savedOrder;
    }

    @Override
    @Transactional
    public Order updateOrder(Long orderId, String orderStatus) throws Exception {
        if (orderStatus == null || !VALID_STATUSES.contains(orderStatus)) {
            throw new OrderStatusException("Please choose a valid order status");
        }
        Order order = findOrderById(orderId);
        order.setOrderStatus(orderStatus);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User user) throws Exception {
        Order order = findOrderById(orderId);

        boolean isCustomer = order.getCustomer() != null
                && Objects.equals(order.getCustomer().getId(), user.getId());
        boolean isRestaurantOwner = order.getRestaurant() != null
                && order.getRestaurant().getOwner() != null
                && Objects.equals(order.getRestaurant().getOwner().getId(), user.getId());

        if (!isCustomer && !isRestaurantOwner) {
            throw new NotAuthorizedException("You are not allowed to cancel this order");
        }
        if (STATUS_DELIVERED.equals(order.getOrderStatus()) || STATUS_COMPLETED.equals(order.getOrderStatus())) {
            throw new OrderStatusException("A " + order.getOrderStatus().toLowerCase() + " order cannot be cancelled");
        }

        order.setOrderStatus(STATUS_CANCELLED);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getUsersOrder(Long userId) {
        return orderRepository.findByCustomerId(userId);
    }

    @Override
    public List<Order> getRestaurantOrder(Long restaurantId, String status) {
        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(order -> status.equals(order.getOrderStatus()))
                    .collect(Collectors.toList());
        }
        return orders;
    }

    @Override
    public Order findOrderById(Long orderId) throws Exception {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    private Address resolveDeliveryAddress(Address requested, User user) {
        for (Address existing : user.getAddresses()) {
            if (sameAddress(existing, requested)) {
                return existing;
            }
        }
        Address saved = addressRepository.save(requested);
        user.getAddresses().add(saved);
        userRepository.save(user);
        return saved;
    }

    private boolean sameAddress(Address a, Address b) {
        return equalsIgnoreCaseNullSafe(a.getStreetAddress(), b.getStreetAddress())
                && equalsIgnoreCaseNullSafe(a.getCity(), b.getCity())
                && equalsIgnoreCaseNullSafe(a.getState(), b.getState())
                && equalsIgnoreCaseNullSafe(a.getPincode(), b.getPincode())
                && equalsIgnoreCaseNullSafe(a.getCountry(), b.getCountry());
    }

    private boolean equalsIgnoreCaseNullSafe(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
