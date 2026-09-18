package com.hellfire.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Customer-raised refund request. Amount defaults to everything still refundable. */
@Data
public class RefundRequest {

    @NotBlank
    @Size(max = 1000)
    private String reason;

    @DecimalMin(value = "0.01", message = "must be positive")
    private BigDecimal amount;

    /** For cash-on-delivery orders; falls back to the customer's saved refund account when omitted. */
    private CustomerBankDetails bankAccount;

    /** Store the supplied bank details as the customer's saved refund account. */
    private boolean saveBankAccount;
}
