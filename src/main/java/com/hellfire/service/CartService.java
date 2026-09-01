package com.hellfire.service;

import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.User;
import com.hellfire.request.AddCartItemRequest;

import java.math.BigDecimal;

public interface CartService {

    CartItem addItemToCart(AddCartItemRequest request, User user) throws Exception;

    CartItem updateCartItemQuantity(Long cartItemId, int quantity, User user) throws Exception;

    Cart removeItemFromCart(Long cartItemId, User user) throws Exception;

    BigDecimal calCartTotal(Cart cart);

    Cart findCartById(Long id) throws Exception;

    Cart findCartByUserId(Long userId) throws Exception;

    Cart clearCart(Long userId) throws Exception;
}
