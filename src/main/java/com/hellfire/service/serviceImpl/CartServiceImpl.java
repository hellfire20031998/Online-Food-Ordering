package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.CartItemNotFoundEException;
import com.hellfire.exceptions.CartNotFoundException;
import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.Food;
import com.hellfire.model.User;
import com.hellfire.repository.CartItemRepository;
import com.hellfire.repository.CartRepository;
import com.hellfire.repository.FoodRepository;
import com.hellfire.request.AddCardItemRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.FoodService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartServiceImpl  implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CartItemRepository CartItemRepository;

    @Autowired
    private FoodService foodService;
    @Autowired
    private CartItemRepository cartItemRepository;


    @Override
    public CartItem addItemToCart(AddCardItemRequest request, String token) throws Exception {

        User user = userService.findUserByJwtToken(token);

        Food food = foodService.findFoodById(request.getFoodId());

        Cart cart = cartRepository.findByCustomerId(user.getId());

        for(CartItem item : cart.getItems()) {
            if(item.getFood().equals(food)) {
                int newQuantity = item.getQuantity() + request.getQuantity();
                return updateCardItemQuantity(item.getId(), newQuantity);
            }
        }
        CartItem cartItem = new CartItem();
        cartItem.setFood(food);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setCart(cart);
        cartItem.setIngredients(request.getIngredients());
        cartItem.setTotalPrice(request.getQuantity()*food.getPrice());

        cart.getItems().add(cartItem);
        return cartItemRepository.save(cartItem);
    }

    @Override
    public CartItem updateCardItemQuantity(Long cartItemId, int quantity) throws Exception {

//        System.out.println("updateCartItemQuantity service" + cartItemId+" "+quantity);
        Optional<CartItem> cartItem = cartItemRepository.findById(cartItemId);

        if(cartItem.isPresent()) {
            CartItem cartItem1 = cartItem.get();

                cartItem1.setQuantity(quantity);
                cartItem1.setTotalPrice(cartItem1.getFood().getPrice() * quantity);
//            System.out.println("updateCartItemQuantity service cartItem"+cartItem1.getId()+" "+cartItem1.getQuantity());
                return cartItemRepository.save(cartItem1);

        }
        throw new CartItemNotFoundEException("Cart item not found");
    }

    @Override
    public Cart removeItemFromCard(Long cartItemId, String token) throws Exception {
      User user = userService.findUserByJwtToken(token);

      Cart cart = cartRepository.findByCustomerId(user.getId());

        Optional<CartItem> optionalCartItem = cartItemRepository.findById(cartItemId);
        if(optionalCartItem.isEmpty()) {
            throw new CartItemNotFoundEException("Cart item not found");
        }
        CartItem cartItem = optionalCartItem.get();
      cart.getItems().remove(cartItem);

      return cartRepository.save(cart);
    }

    @Override
    public Long calCartTotal(Cart cart) throws Exception {
        Long total=0L;

        for(CartItem item : cart.getItems()) {
            total += item.getFood().getPrice()*item.getQuantity();
        }
        return total;
    }

    @Override
    public Cart findCartById(Long id) throws Exception {
        Optional<Cart> cart = cartRepository.findById(id);

        if(cart.isEmpty()) {
            throw new CartNotFoundException("Cart not found");
        }
        return cart.get();
    }

    @Override
    public Cart findCartByUserId(Long userId) throws Exception {

//        User userId= userService.findUserByJwtToken();
        Cart cart = cartRepository.findByCustomerId(userId);
        cart.setTotal(calCartTotal(cart));
        return cart;
    }

    @Override
    public Cart clearCart(Long userId) throws Exception {

      Cart cart =cartRepository.findByCustomerId(userId);
      cart.getItems().clear();
      return cartRepository.save(cart);
    }
}
