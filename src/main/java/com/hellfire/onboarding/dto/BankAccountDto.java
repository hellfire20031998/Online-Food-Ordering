package com.hellfire.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Account number and UPI id are masked unless the caller is entitled to see them. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto {

    private String accountHolderName;
    private String accountNumber;
    private String ifsc;
    private String bankName;
    private String upiId;
    private boolean masked;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
