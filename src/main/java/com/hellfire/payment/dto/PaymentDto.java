package com.hellfire.payment.dto;

import com.hellfire.model.PaymentMethods;
import com.hellfire.model.PaymentProvider;
import com.hellfire.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;
    private Long orderId;
    private PaymentMethods method;
    private PaymentProvider provider;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;
    private String currency;
    private String providerPaymentId;
    /** Only present on responses to the paying customer while the payment is pending. */
    private String clientSecret;
    private LocalDateTime paidAt;
    private String failureReason;
    private boolean hasOpenRefund;
    private BigDecimal commissionPercentage;
    private BigDecimal commissionAmount;
}
