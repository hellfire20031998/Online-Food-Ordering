package com.hellfire.controller;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.model.Restaurant;
import com.hellfire.model.RestaurantRole;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.repository.RestaurantRoleRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.response.MessageResponse;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/restaurant-roles")
@RequiredArgsConstructor
public class RestaurantRoleController {

    private final RestaurantRoleRepository restaurantRoleRepository;
    private final UserRepository userRepository;
    private final RestaurantService restaurantService;
    private final UserService userService;

    @PostMapping("/assign")
    public ResponseEntity<MessageResponse> assignRole(
            @RequestParam Long userId,
            @RequestParam Long restaurantId,
            @RequestParam String role,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {

        User currentUser = userService.findUserByJwtToken(token);
        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);

        boolean isOwner = restaurant.getOwner() != null
                && Objects.equals(restaurant.getOwner().getId(), currentUser.getId());
        if (!isOwner) {
            throw new NotAuthorizedException("Only the restaurant owner can assign roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserRole enumRole;
        try {
            enumRole = UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        if (enumRole != UserRole.MANAGER && enumRole != UserRole.MEMBER) {
            throw new IllegalArgumentException("Only MANAGER or MEMBER can be assigned to a restaurant");
        }

        RestaurantRole restaurantRole = new RestaurantRole();
        restaurantRole.setRestaurant(restaurant);
        restaurantRole.setUser(user);
        restaurantRole.setRole(enumRole);
        restaurantRoleRepository.save(restaurantRole);

        return ResponseEntity.ok(new MessageResponse("Role assigned successfully"));
    }
}
