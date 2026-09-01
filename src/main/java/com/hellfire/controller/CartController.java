package com.hellfire.controller;

import com.hellfire.cart.dto.CartDto;
import com.hellfire.cart.dto.CartItemDto;
import com.hellfire.cart.mapper.CartMapper;
import com.hellfire.model.User;
import com.hellfire.request.AddCartItemRequest;
import com.hellfire.request.UpdateCartItemRequest;
import com.hellfire.service.CartService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/cart/add")
    public ResponseEntity<CartItemDto> addItemToCart(@Valid @RequestBody AddCartItemRequest request,
                                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toItemDto(cartService.addItemToCart(request, user)));
    }

    @PutMapping("/cart-item/update")
    public ResponseEntity<CartItemDto> updateCartItemQuantity(@Valid @RequestBody UpdateCartItemRequest request,
                                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toItemDto(
                cartService.updateCartItemQuantity(request.getCartItemId(), request.getQuantity(), user)));
    }

    @DeleteMapping("/cart-item/{id}/remove")
    public ResponseEntity<CartDto> removeCartItem(@PathVariable Long id,
                                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toDto(cartService.removeItemFromCart(id, user)));
    }

    @DeleteMapping("/cart/clear")
    public ResponseEntity<CartDto> clearCart(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toDto(cartService.clearCart(user.getId())));
    }

    @GetMapping("/cart/")
    public ResponseEntity<CartDto> findUserCart(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(CartMapper.toDto(cartService.findCartByUserId(user.getId())));
    }
}
