package com.hellfire.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BankAccountRequest {

    @NotBlank
    private String accountHolderName;

    @NotBlank
    @Pattern(regexp = "\\d{9,18}", message = "must be 9 to 18 digits")
    private String accountNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "must be a valid IFSC code")
    private String ifsc;

    @NotBlank
    private String bankName;

    /** Optional. Validated in the service when present. */
    private String upiId;
}
