package com.hellfire.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Team-initiated refund; created and approved in one step. */
@Data
public class TeamRefundRequest {

    @NotNull
    private Long orderId;

    @DecimalMin(value = "0.01", message = "must be positive")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 1000)
    private String reason;

    /** Required for cash-on-delivery orders. */
    private CustomerBankDetails bankAccount;

    @Size(max = 1000)
    private String notes;
}
