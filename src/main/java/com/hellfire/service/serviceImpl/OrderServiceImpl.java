package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.OrderNotFoundException;
import com.hellfire.exceptions.OrderStatusException;
import com.hellfire.model.*;
import com.hellfire.payment.service.PaymentService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final RestaurantService restaurantService;
    private final CartService cartService;
    private final PaymentService paymentService;

    /**
     * Creates the order and its payment. Cash on delivery goes straight to PENDING and empties the
     * cart; online payment leaves the order in PAYMENT_PENDING (hidden from the restaurant) until
     * the gateway confirms, and the cart is cleared only then.
     */
    @Override
    @Transactional
    public Order createOrder(OrderRequest request, User user) throws Exception {
        PaymentMethods paymentMethod = PaymentMethods.fromString(request.getPaymentMethod());
        Restaurant restaurant = restaurantService.findRestaurantById(request.getRestaurantId());
        if (restaurant.isSuspended()) {
            throw new IllegalArgumentException("This restaurant is currently unavailable");
        }

        Cart cart = cartService.findCartByUserId(user.getId());
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart");
        }

        Address address = resolveDeliveryAddress(request.getDeliveryAddress(), user);
        boolean cashOnDelivery = paymentMethod == PaymentMethods.CASH_ON_DELIVERY;

        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCustomer(user);
        order.setDeliveryAddress(address);
        order.setCreatedAt(new Date());
        order.setOrderStatus(cashOnDelivery ? OrderStatus.PENDING : OrderStatus.PAYMENT_PENDING);
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
        savedOrder.setPayment(paymentService.createForOrder(savedOrder, paymentMethod));

        if (cashOnDelivery) {
            cartService.clearCart(user.getId());
        }
        return savedOrder;
    }

    /** Restaurant-side status changes. Payment states belong to the payment system. */
    @Override
    @Transactional
    public Order updateOrder(Long orderId, OrderStatus orderStatus) throws Exception {
        if (orderStatus == null) {
            throw new OrderStatusException("Please choose a valid order status");
        }
        if (orderStatus.isPaymentState()) {
            throw new OrderStatusException("Payment states are managed by the payment system");
        }
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != null && order.getOrderStatus().isPaymentState()) {
            throw new OrderStatusException("This order is still awaiting payment");
        }
        order.setOrderStatus(orderStatus);
        Order saved = orderRepository.save(order);
        if (orderStatus.isFulfilled()) {
            paymentService.onOrderFulfilled(saved);
        }
        return saved;
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
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new OrderStatusException("This order is already cancelled");
        }
        if (order.getOrderStatus() != null && order.getOrderStatus().isFulfilled()) {
            throw new OrderStatusException(
                    "A " + order.getOrderStatus().name().toLowerCase() + " order cannot be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        paymentService.onOrderCancelled(saved, user.getEmail(), isCustomer);
        return saved;
    }

    @Override
    public List<Order> getUsersOrder(Long userId) {
        return orderRepository.findByCustomerId(userId);
    }

    /** Restaurant view: unpaid online orders are hidden unless that status is asked for explicitly. */
    @Override
    public List<Order> getRestaurantOrder(Long restaurantId, OrderStatus status) {
        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        if (status != null) {
            return orders.stream()
                    .filter(order -> status == order.getOrderStatus())
                    .collect(Collectors.toList());
        }
        return orders.stream()
                .filter(order -> order.getOrderStatus() == null || !order.getOrderStatus().isPaymentState())
                .collect(Collectors.toList());
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
