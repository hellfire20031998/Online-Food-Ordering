package com.hellfire.controller;


import com.hellfire.model.User;
import com.hellfire.cart.dto.CartDto;
import com.hellfire.cart.dto.CartItemDto;
import com.hellfire.cart.mapper.CartMapper;
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
    public ResponseEntity<CartItemDto> addItemToCart(@RequestBody AddCardItemRequest cartItem,
                                                  @RequestHeader("Authorization") String token) throws Exception {
        return ResponseEntity.ok(CartMapper.toItemDto(cartService.addItemToCart(cartItem, token)));
    }

    @PutMapping("/cart-item/update")
    public ResponseEntity<CartItemDto> updateCartItemQuantity(@RequestBody UpdateCartItemRequest request,
                                                          @RequestHeader("Authorization") String token) throws Exception {
        return ResponseEntity.ok(
                CartMapper.toItemDto(cartService.updateCardItemQuantity(request.getCartId(), request.getQuantity()))
        );
    }


    @DeleteMapping("/cart-item/{id}/remove")
    public ResponseEntity<CartDto> removeCartItem( @PathVariable Long id,
                                                @RequestHeader("Authorization") String token) throws Exception {
        return ResponseEntity.ok(CartMapper.toDto(cartService.removeItemFromCard(id,token)));
    }

    @DeleteMapping("/cart/clear")
    public ResponseEntity<CartDto> clearCart(@RequestHeader("Authorization") String token) throws Exception {
        User user=userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toDto(cartService.clearCart(user.getId())));
    }
    @GetMapping("/cart/")
    public ResponseEntity<CartDto>  findUserCart(@RequestHeader("Authorization") String token) throws Exception {
        User user=userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toDto(cartService.findCartByUserId(user.getId())));
    }




}
