package com.hellfire.payment.dto;

import com.hellfire.model.PayoutStatus;
import com.hellfire.onboarding.dto.BankAccountDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDto {

    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int paymentCount;
    private BigDecimal grossAmount;
    private BigDecimal refundedAmount;
    private BigDecimal commissionPercentage;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private String currency;
    private PayoutStatus status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime paidAt;
    private String paidBy;
    private String referenceNumber;
    private String notes;
    /** Destination snapshot; masked unless the caller may see it. Null when none was on file. */
    private BankAccountDto bankAccount;
    /** Only on the detail view. */
    private List<PaymentDto> payments;
}
