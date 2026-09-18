package com.hellfire.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarkPayoutPaidRequest {

    @NotBlank(message = "the bank transfer reference is required")
    @Size(max = 200)
    private String referenceNumber;

    @Size(max = 1000)
    private String notes;
}
