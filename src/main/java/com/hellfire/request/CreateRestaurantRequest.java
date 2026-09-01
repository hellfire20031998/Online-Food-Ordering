package com.hellfire.request;

import com.hellfire.model.Address;
import com.hellfire.model.ContactInformation;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateRestaurantRequest {

    private Long id;

    @NotBlank
    private String name;

    private String description;
    private String cuisineType;
    private Address address;
    private ContactInformation contactInformation;
    private String openingHours;
    private List<String> images;
}
