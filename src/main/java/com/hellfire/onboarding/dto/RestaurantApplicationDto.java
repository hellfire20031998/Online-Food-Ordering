package com.hellfire.onboarding.dto;

import com.hellfire.model.ApplicationStatus;
import com.hellfire.model.ContactInformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantApplicationDto {

    private Long id;
    private ApplicationStatus status;

    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    private String restaurantName;
    private String description;
    private String cuisineType;
    private String openingHours;
    private String streetAddress;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private ContactInformation contact;
    private List<String> images;

    private BankAccountDto bankAccount;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String rejectionReason;
    private Long restaurantId;
}
