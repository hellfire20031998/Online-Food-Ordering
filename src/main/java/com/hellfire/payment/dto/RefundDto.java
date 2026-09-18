package com.hellfire.payment.dto;

import com.hellfire.model.RefundMethod;
import com.hellfire.model.RefundStatus;
import com.hellfire.onboarding.dto.BankAccountDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto {

    private Long id;
    private Long paymentId;
    private Long orderId;
    private Long restaurantId;
    private String restaurantName;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private RefundStatus status;
    private RefundMethod method;
    private String providerRefundId;
    private String requestedBy;
    private boolean requestedByCustomer;
    private LocalDateTime requestedAt;
    private String processedBy;
    private LocalDateTime processedAt;
    private String referenceNumber;
    private String notes;
    private String rejectionReason;
    /** Customer's destination for bank-transfer refunds; masked unless the caller may see it. */
    private BankAccountDto bankAccount;
}
