package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.CartItemNotFoundException;
import com.hellfire.exceptions.CartNotFoundException;
import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.Food;
import com.hellfire.model.User;
import com.hellfire.repository.CartItemRepository;
import com.hellfire.repository.CartRepository;
import com.hellfire.request.AddCartItemRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodService foodService;

    @Override
    @Transactional
    public CartItem addItemToCart(AddCartItemRequest request, User user) throws Exception {
        Food food = foodService.findFoodById(request.getFoodId());
        if (!food.isAvailable()) {
            throw new IllegalArgumentException("This item is currently unavailable");
        }

        Cart cart = requireCartByUserId(user.getId());

        for (CartItem item : cart.getItems()) {
            if (item.getFood().getId().equals(food.getId())) {
                int newQuantity = item.getQuantity() + request.getQuantity();
                return updateCartItemQuantity(item.getId(), newQuantity, user);
            }
        }

        CartItem cartItem = new CartItem();
        cartItem.setFood(food);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setCart(cart);
        cartItem.setIngredients(request.getIngredients());
        cartItem.setTotalPrice(itemTotal(food, request.getQuantity()));

        cart.getItems().add(cartItem);
        return cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public CartItem updateCartItemQuantity(Long cartItemId, int quantity, User user) throws Exception {
        CartItem cartItem = requireOwnedCartItem(cartItemId, user);
        cartItem.setQuantity(quantity);
        cartItem.setTotalPrice(itemTotal(cartItem.getFood(), quantity));
        return cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public Cart removeItemFromCart(Long cartItemId, User user) throws Exception {
        CartItem cartItem = requireOwnedCartItem(cartItemId, user);
        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);
        cart.setTotal(calCartTotal(cart));
        return cartRepository.save(cart);
    }

    @Override
    public BigDecimal calCartTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            total = total.add(itemTotal(item.getFood(), item.getQuantity()));
        }
        return total;
    }

    @Override
    public Cart findCartById(Long id) throws Exception {
        return cartRepository.findById(id)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
    }

    @Override
    public Cart findCartByUserId(Long userId) throws Exception {
        Cart cart = requireCartByUserId(userId);
        cart.setTotal(calCartTotal(cart));
        return cart;
    }

    @Override
    @Transactional
    public Cart clearCart(Long userId) throws Exception {
        Cart cart = requireCartByUserId(userId);
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }

    private Cart requireCartByUserId(Long userId) throws CartNotFoundException {
        Cart cart = cartRepository.findByCustomerId(userId);
        if (cart == null) {
            throw new CartNotFoundException("Cart not found for user");
        }
        return cart;
    }

    private CartItem requireOwnedCartItem(Long cartItemId, User user) throws CartItemNotFoundException {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));
        Long ownerId = cartItem.getCart().getCustomer().getId();
        if (!ownerId.equals(user.getId())) {
            throw new NotAuthorizedException("This cart item does not belong to you");
        }
        return cartItem;
    }

    private BigDecimal itemTotal(Food food, int quantity) {
        BigDecimal price = food.getPrice() == null ? BigDecimal.ZERO : food.getPrice();
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
