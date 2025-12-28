package com.hellfire.controller;

import com.hellfire.model.Restaurant;
import com.hellfire.model.RestaurantRole;
import com.hellfire.model.USER_ROLE;
import com.hellfire.model.User;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.RestaurantRoleRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.UserAndRoleRequest;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurant-roles")
@RequiredArgsConstructor
public class RestaurantRoleController {

    @Autowired
    private final RestaurantRoleRepository restaurantRoleRepository;
    @Autowired
    private final RestaurantRepository restaurantRepository;
    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserService userService;

    @PostMapping("/assign")
    public ResponseEntity<?> assignRole(
            @RequestParam Long userId,
            @RequestParam Long restaurantId,
            @RequestParam String role,
            @AuthenticationPrincipal User currentUser) {

        System.out.println("Assigning role: " + role + " to user: " + userId + " for restaurant: " + restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (!restaurant.getOwner().getId().equals(currentUser.getId())
                && currentUser.getRole() != USER_ROLE.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to assign roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        USER_ROLE enumRole;
        try {
            enumRole = USER_ROLE.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role: " + role);
        }

        RestaurantRole restaurantRole = new RestaurantRole();
        restaurantRole.setRestaurant(restaurant);
        restaurantRole.setUser(user);
        restaurantRole.setRole(enumRole);

        restaurantRoleRepository.save(restaurantRole);

        return ResponseEntity.ok("Role assigned successfully");
    }



//    @GetMapping("/my-role/{restaurantId}")
//    public ResponseEntity<?> getMyRoleForRestaurant(
//            @PathVariable Long restaurantId,
//            @AuthenticationPrincipal User currentUser,
//            @RequestHeader("Authorization") String token) throws Exception {
//
//        User user= userService.findUserByJwtToken(token);
//
//           Restaurant restaurant= restaurantService.getRestaurantByUserId(user.getId());
//           if(restaurant==null){
//
//           }
//
//    }


}
