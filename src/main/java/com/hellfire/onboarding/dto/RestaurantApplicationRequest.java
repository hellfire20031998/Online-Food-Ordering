package com.hellfire.onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RestaurantApplicationRequest {

    @NotBlank
    private String restaurantName;

    @Size(max = 1000)
    private String description;

    @NotBlank
    private String cuisineType;

    private String openingHours;

    private String applicantPhone;

    @NotNull
    @Valid
    private AddressPart address;

    @NotNull
    @Valid
    private ContactPart contact;

    private List<String> images;

    @NotNull
    @Valid
    private BankAccountRequest bankAccount;

    @Data
    public static class AddressPart {
        @NotBlank private String streetAddress;
        @NotBlank private String city;
        @NotBlank private String state;
        @NotBlank private String pincode;
        @NotBlank private String country;
    }

    @Data
    public static class ContactPart {
        @NotBlank @Email private String email;
        @NotBlank private String mobile;
        private String twitter;
        private String instagram;
    }
}
