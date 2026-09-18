package com.hellfire.payment.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body for approve / complete / reject on a refund. Fields are used per action. */
@Data
public class RefundDecisionRequest {

    /** approve: optional bank details when the customer did not provide them. */
    private CustomerBankDetails bankAccount;

    /** complete: bank transaction reference. */
    @Size(max = 200)
    private String referenceNumber;

    /** reject: reason sent to the customer. */
    @Size(max = 1000)
    private String reason;

    @Size(max = 1000)
    private String notes;
}
