package com.hellfire.service;


import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.request.AddCardItemRequest;

public interface CartService {

    CartItem addItemToCart(AddCardItemRequest request, String token) throws Exception;

    CartItem updateCardItemQuantity(Long cartItemId, int quantity) throws Exception;

    Cart removeItemFromCard(Long cartItemId,String token) throws Exception;

    Long calCartTotal(Cart cart) throws Exception;

    Cart findCartById(Long id) throws Exception;

    Cart findCartByUserId(Long userId) throws Exception;

    Cart clearCart(Long cartId) throws Exception;








}
