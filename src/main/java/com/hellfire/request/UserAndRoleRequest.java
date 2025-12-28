package com.hellfire.request;

import com.hellfire.model.Restaurant;
import com.hellfire.model.USER_ROLE;
import com.hellfire.model.User;
import lombok.Data;

@Data
public class UserAndRoleRequest {

     private Long userId;
     private String role;
     private  Long restaurantId;

     public USER_ROLE getEnumRole() {
          try {
               return role == null ? null : USER_ROLE.valueOf(role.trim().toUpperCase());
          } catch (IllegalArgumentException e) {
               throw new RuntimeException("Invalid role value: " + role);
          }
     }
}
