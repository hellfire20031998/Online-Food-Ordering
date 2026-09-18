package com.hellfire.team.dto;

import com.hellfire.model.RestaurantStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRestaurantDto {

    private Long id;
    private String name;
    private String cuisineType;
    private String city;
    private String description;
    private String openingHours;
    private List<String> images;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private boolean open;
    private RestaurantStatus status;
    private LocalDateTime registrationDate;
    private long totalOrders;
}
