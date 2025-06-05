package com.hellfire.controller;


import com.hellfire.model.Cart;
import com.hellfire.model.CartItem;
import com.hellfire.model.User;
import com.hellfire.request.AddCardItemRequest;
import com.hellfire.request.UpdateCartItemRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @PostMapping("/cart/add")
    public ResponseEntity<CartItem> addItemToCart(@RequestBody AddCardItemRequest cartItem,
                                                  @RequestHeader("Authorization") String token) throws Exception {
        CartItem cartItems= cartService.addItemToCart(cartItem,token);

        System.out.println("cartItem: "+cartItem);
        return ResponseEntity.ok(cartItems);
    }

    @PutMapping("/cart-item/update")
    public ResponseEntity<CartItem> updateCartItemQuantity(@RequestBody UpdateCartItemRequest request,
                                                          @RequestHeader("Authorization") String token) throws Exception {
//        System.out.println("updateCartItemQuantity: "+request);
        CartItem cartItem= cartService.updateCardItemQuantity(request.getCartId(), request.getQuantity());
//        System.out.println(" updated cartItem: "+cartItem);
        return ResponseEntity.ok(cartItem);
    }


    @DeleteMapping("/cart-item/{id}/remove")
    public ResponseEntity<Cart> removeCartItem( @PathVariable Long id,
                                                @RequestHeader("Authorization") String token) throws Exception {
        Cart cart= cartService.removeItemFromCard(id,token);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/cart/clear")
    public ResponseEntity<?> clearCart(@RequestHeader("Authorization") String token) throws Exception {
        User user=userService.findUserByJwtToken(token);
        Cart cart= cartService.clearCart(user.getId());

        return ResponseEntity.ok(cart);
    }
    @GetMapping("/cart/")
    public ResponseEntity<?>  findUserCart(@RequestHeader("Authorization") String token) throws Exception {
        User user=userService.findUserByJwtToken(token);
        Cart cart = cartService.findCartByUserId(user.getId());

        return ResponseEntity.ok(cart);
    }




}
