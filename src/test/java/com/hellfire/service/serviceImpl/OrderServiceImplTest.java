package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.OrderStatusException;
import com.hellfire.model.*;
import com.hellfire.repository.AddressRepository;
import com.hellfire.repository.OrderItemRepository;
import com.hellfire.repository.OrderRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.OrderRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestaurantService restaurantService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User customer;
    private Restaurant restaurant;
    private Cart cart;
    private OrderRequest request;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setAddresses(new ArrayList<>());

        User owner = new User();
        owner.setId(2L);

        restaurant = new Restaurant();
        restaurant.setId(5L);
        restaurant.setOwner(owner);

        Food food = new Food();
        food.setId(100L);
        food.setPrice(new BigDecimal("50.00"));

        CartItem item = new CartItem();
        item.setFood(food);
        item.setQuantity(2);
        item.setTotalPrice(new BigDecimal("100.00"));

        cart = new Cart();
        cart.setCustomer(customer);
        cart.setItems(new ArrayList<>());
        cart.getItems().add(item);

        Address address = new Address();
        address.setStreetAddress("1 Main St");
        address.setCity("Pune");

        request = new OrderRequest();
        request.setRestaurantId(5L);
        request.setDeliveryAddress(address);
        request.setPaymentMethod("CASH_ON_DELIVERY");
    }

    @Test
    void createOrderSetsTotalsAndClearsCart() throws Exception {
        when(restaurantService.findRestaurantById(5L)).thenReturn(restaurant);
        when(cartService.findCartByUserId(1L)).thenReturn(cart);
        when(cartService.calCartTotal(cart)).thenReturn(new BigDecimal("100.00"));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(request, customer);

        assertEquals(0, new BigDecimal("100.00").compareTo(order.getTotalPrice()));
        assertEquals(0, new BigDecimal("100.00").compareTo(order.getTotalAmount()));
        assertEquals(1L, order.getTotalItems());
        assertEquals(OrderServiceImpl.STATUS_PENDING, order.getOrderStatus());
        assertEquals(PaymentMethods.CASH_ON_DELIVERY, order.getPaymentMethod());
        verify(cartService).clearCart(1L);
    }

    @Test
    void createOrderRejectsEmptyCart() throws Exception {
        cart.getItems().clear();
        when(restaurantService.findRestaurantById(5L)).thenReturn(restaurant);
        when(cartService.findCartByUserId(1L)).thenReturn(cart);

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request, customer));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsInvalidPaymentMethod() {
        request.setPaymentMethod("BARTER");
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request, customer));
    }

    @Test
    void createOrderReusesMatchingAddress() throws Exception {
        Address existing = new Address();
        existing.setStreetAddress("1 Main St");
        existing.setCity("Pune");
        customer.getAddresses().add(existing);

        when(restaurantService.findRestaurantById(5L)).thenReturn(restaurant);
        when(cartService.findCartByUserId(1L)).thenReturn(cart);
        when(cartService.calCartTotal(cart)).thenReturn(new BigDecimal("100.00"));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(request, customer);

        assertSame(existing, order.getDeliveryAddress());
        assertEquals(1, customer.getAddresses().size());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void customerCanCancelOwnPendingOrder() throws Exception {
        Order order = pendingOrder();
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order cancelled = orderService.cancelOrder(9L, customer);

        assertEquals(OrderServiceImpl.STATUS_CANCELLED, cancelled.getOrderStatus());
    }

    @Test
    void strangerCannotCancelOrder() {
        Order order = pendingOrder();
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        User stranger = new User();
        stranger.setId(99L);

        assertThrows(NotAuthorizedException.class, () -> orderService.cancelOrder(9L, stranger));
    }

    @Test
    void deliveredOrderCannotBeCancelled() {
        Order order = pendingOrder();
        order.setOrderStatus(OrderServiceImpl.STATUS_DELIVERED);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        assertThrows(OrderStatusException.class, () -> orderService.cancelOrder(9L, customer));
    }

    @Test
    void updateOrderRejectsUnknownStatus() {
        assertThrows(OrderStatusException.class, () -> orderService.updateOrder(9L, "SHIPPED"));
        verify(orderRepository, never()).findById(anyLong());
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(9L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setOrderStatus(OrderServiceImpl.STATUS_PENDING);
        return order;
    }
}
