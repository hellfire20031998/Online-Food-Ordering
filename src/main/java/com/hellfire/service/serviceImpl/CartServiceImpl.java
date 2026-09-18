package com.hellfire.service.serviceImpl;

import com.hellfire.cart.dto.CartMergeResponse;
import com.hellfire.cart.mapper.CartMapper;
import com.hellfire.exceptions.CartConflictException;
import com.hellfire.exceptions.CartItemNotFoundException;
import com.hellfire.exceptions.CartNotFoundException;
import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.Food;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.repository.CartItemRepository;
import com.hellfire.repository.CartRepository;
import com.hellfire.request.AddCartItemRequest;
import com.hellfire.request.CartMergeRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A cart belongs to one customer and holds dishes from one restaurant at a time. Adding from a
 * different restaurant is refused with a 409 unless the caller asks to replace the cart.
 */
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
        enforceSingleRestaurant(cart, food, request.isReplaceCart());
        return addOrIncrement(cart, food, request.getQuantity(), request.getIngredients(), user);
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

    /**
     * Guest items are validated against the live menu one by one: unknown, unavailable or
     * other-restaurant dishes are skipped and reported rather than failing the whole merge.
     */
    @Override
    @Transactional
    public CartMergeResponse mergeGuestItems(CartMergeRequest request, User user) throws Exception {
        Cart cart = requireCartByUserId(user.getId());
        List<CartMergeResponse.SkippedItem> skipped = new ArrayList<>();
        List<CartMergeRequest.GuestItem> guestItems = request.getItems() == null ? List.of() : request.getItems();

        switch (request.getStrategy()) {
            case KEEP_SERVER -> {
                // nothing to do
            }
            case REPLACE -> {
                cart.getItems().clear();
                addGuestItems(cart, guestItems, user, skipped, false);
            }
            case MERGE -> {
                // If the account already holds items, their restaurant wins and a clash is an error
                // the client must resolve; otherwise the first guest item decides the restaurant.
                boolean accountCartHadItems = !cart.getItems().isEmpty();
                addGuestItems(cart, guestItems, user, skipped, accountCartHadItems);
            }
        }

        cart.setTotal(calCartTotal(cart));
        Cart saved = cartRepository.save(cart);
        return new CartMergeResponse(CartMapper.toDto(saved), skipped);
    }

    // ------------------------------------------------------------------ internals

    private void addGuestItems(Cart cart, List<CartMergeRequest.GuestItem> guestItems, User user,
                               List<CartMergeResponse.SkippedItem> skipped, boolean conflictIsError) throws Exception {
        for (CartMergeRequest.GuestItem guest : guestItems) {
            Food food;
            try {
                food = foodService.findFoodById(guest.getFoodId());
            } catch (Exception e) {
                skipped.add(new CartMergeResponse.SkippedItem(guest.getFoodId(), null, "No longer on the menu"));
                continue;
            }
            if (!food.isAvailable()) {
                skipped.add(new CartMergeResponse.SkippedItem(food.getId(), food.getName(), "Currently unavailable"));
                continue;
            }
            Restaurant current = cartRestaurant(cart);
            if (current != null && food.getRestaurant() != null
                    && !Objects.equals(current.getId(), food.getRestaurant().getId())) {
                if (conflictIsError) {
                    throw new CartConflictException(current.getName(), food.getRestaurant().getName());
                }
                skipped.add(new CartMergeResponse.SkippedItem(food.getId(), food.getName(),
                        "From a different restaurant than the rest of the cart"));
                continue;
            }
            addOrIncrement(cart, food, Math.max(1, guest.getQuantity()), guest.getIngredients(), user);
        }
    }

    private void enforceSingleRestaurant(Cart cart, Food food, boolean replace) {
        Restaurant current = cartRestaurant(cart);
        if (current == null || food.getRestaurant() == null
                || Objects.equals(current.getId(), food.getRestaurant().getId())) {
            return;
        }
        if (!replace) {
            throw new CartConflictException(current.getName(), food.getRestaurant().getName());
        }
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
    }

    /** Same dish with the same ingredient choices bumps the quantity; anything else is a new line. */
    private CartItem addOrIncrement(Cart cart, Food food, int quantity, List<String> ingredients, User user) throws Exception {
        Set<String> wanted = new HashSet<>(ingredients == null ? List.of() : ingredients);
        for (CartItem item : cart.getItems()) {
            boolean sameFood = item.getFood() != null && Objects.equals(item.getFood().getId(), food.getId());
            boolean sameIngredients = new HashSet<>(item.getIngredients() == null ? List.of() : item.getIngredients()).equals(wanted);
            if (sameFood && sameIngredients) {
                int newQuantity = item.getQuantity() + quantity;
                item.setQuantity(newQuantity);
                item.setTotalPrice(itemTotal(food, newQuantity));
                return item.getId() == null ? item : cartItemRepository.save(item);
            }
        }

        CartItem cartItem = new CartItem();
        cartItem.setFood(food);
        cartItem.setQuantity(quantity);
        cartItem.setCart(cart);
        cartItem.setIngredients(ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients));
        cartItem.setTotalPrice(itemTotal(food, quantity));
        cart.getItems().add(cartItem);
        return cartItemRepository.save(cartItem);
    }

    private static Restaurant cartRestaurant(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::getFood)
                .filter(f -> f != null && f.getRestaurant() != null)
                .map(Food::getRestaurant)
                .findFirst()
                .orElse(null);
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
