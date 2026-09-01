package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.Food;
import com.hellfire.model.User;
import com.hellfire.repository.CartItemRepository;
import com.hellfire.repository.CartRepository;
import com.hellfire.service.FoodService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private FoodService foodService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User owner;
    private User otherUser;
    private Cart cart;
    private CartItem cartItem;
    private Food food;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);

        otherUser = new User();
        otherUser.setId(2L);

        cart = new Cart();
        cart.setId(10L);
        cart.setCustomer(owner);
        cart.setItems(new ArrayList<>());

        food = new Food();
        food.setId(100L);
        food.setPrice(new BigDecimal("50.00"));
        food.setAvailable(true);

        cartItem = new CartItem();
        cartItem.setId(1000L);
        cartItem.setCart(cart);
        cartItem.setFood(food);
        cartItem.setQuantity(1);
        cartItem.setTotalPrice(new BigDecimal("50.00"));
        cart.getItems().add(cartItem);
    }

    @Test
    void updateQuantityRecalculatesTotalPrice() throws Exception {
        when(cartItemRepository.findById(1000L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem updated = cartService.updateCartItemQuantity(1000L, 3, owner);

        assertEquals(3, updated.getQuantity());
        assertEquals(0, new BigDecimal("150.00").compareTo(updated.getTotalPrice()));
    }

    @Test
    void updateQuantityRejectsForeignCartItem() {
        when(cartItemRepository.findById(1000L)).thenReturn(Optional.of(cartItem));

        assertThrows(NotAuthorizedException.class,
                () -> cartService.updateCartItemQuantity(1000L, 3, otherUser));
    }

    @Test
    void removeItemRejectsForeignCartItem() {
        when(cartItemRepository.findById(1000L)).thenReturn(Optional.of(cartItem));

        assertThrows(NotAuthorizedException.class,
                () -> cartService.removeItemFromCart(1000L, otherUser));
    }

    @Test
    void addUnavailableFoodIsRejected() throws Exception {
        food.setAvailable(false);
        when(foodService.findFoodById(100L)).thenReturn(food);

        com.hellfire.request.AddCartItemRequest request = new com.hellfire.request.AddCartItemRequest();
        request.setFoodId(100L);
        request.setQuantity(1);

        assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(request, owner));
    }

    @Test
    void calCartTotalSumsItems() {
        CartItem second = new CartItem();
        second.setFood(food);
        second.setQuantity(2);
        cart.getItems().add(second);

        assertEquals(0, new BigDecimal("150.00").compareTo(cartService.calCartTotal(cart)));
    }
}
