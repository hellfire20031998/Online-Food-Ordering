package com.hellfire.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Saved refund destination. Either account number + IFSC or a UPI id must be present (checked in the service). */
@Data
public class CustomerBankAccountRequest {

    @NotBlank
    private String accountHolderName;

    private String accountNumber;
    private String ifsc;
    private String bankName;
    private String upiId;
}
