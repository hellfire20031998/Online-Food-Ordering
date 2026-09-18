package com.hellfire.onboarding;

import com.hellfire.model.RestaurantApplication;
import com.hellfire.model.RestaurantBankAccount;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.onboarding.dto.RestaurantApplicationDto;

import java.util.ArrayList;

public final class ApplicationMapper {

    private ApplicationMapper() {
    }

    /** @param revealBank whether the caller may see the unmasked account number and UPI id. */
    public static RestaurantApplicationDto toDto(RestaurantApplication a, boolean revealBank) {
        return RestaurantApplicationDto.builder()
                .id(a.getId())
                .status(a.getStatus())
                .applicantId(a.getApplicant() != null ? a.getApplicant().getId() : null)
                .applicantName(a.getApplicant() != null ? a.getApplicant().getFullName() : null)
                .applicantEmail(a.getApplicant() != null ? a.getApplicant().getEmail() : null)
                .applicantPhone(a.getApplicantPhone())
                .restaurantName(a.getRestaurantName())
                .description(a.getDescription())
                .cuisineType(a.getCuisineType())
                .openingHours(a.getOpeningHours())
                .streetAddress(a.getStreetAddress())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .country(a.getCountry())
                .contact(a.getContactInformation())
                .images(a.getImages() == null ? new ArrayList<>() : new ArrayList<>(a.getImages()))
                .bankAccount(bankDto(a.getBankAccountHolderName(), a.getBankAccountNumber(), a.getBankIfsc(),
                        a.getBankName(), a.getUpiId(), null, null, revealBank))
                .submittedAt(a.getSubmittedAt())
                .reviewedAt(a.getReviewedAt())
                .reviewedBy(a.getReviewedBy())
                .rejectionReason(a.getRejectionReason())
                .restaurantId(a.getRestaurant() != null ? a.getRestaurant().getId() : null)
                .build();
    }

    public static BankAccountDto toDto(RestaurantBankAccount b, boolean reveal) {
        return bankDto(b.getAccountHolderName(), b.getAccountNumber(), b.getIfsc(), b.getBankName(),
                b.getUpiId(), b.getUpdatedAt(), b.getUpdatedBy(), reveal);
    }

    private static BankAccountDto bankDto(String holder, String number, String ifsc, String bank, String upi,
                                          java.time.LocalDateTime updatedAt, String updatedBy, boolean reveal) {
        if (holder == null && number == null) {
            return null;
        }
        return new BankAccountDto(
                holder,
                reveal ? number : BankDetails.maskAccountNumber(number),
                ifsc,
                bank,
                reveal ? upi : BankDetails.maskUpi(upi),
                !reveal,
                updatedAt,
                updatedBy);
    }
}
