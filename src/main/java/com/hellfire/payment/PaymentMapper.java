package com.hellfire.payment;

import com.hellfire.model.Payment;
import com.hellfire.model.Payout;
import com.hellfire.model.Refund;
import com.hellfire.onboarding.BankDetails;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.payment.dto.PaymentDto;
import com.hellfire.payment.dto.PayoutDto;
import com.hellfire.payment.dto.RefundDto;

import java.util.List;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentDto toDto(Payment p, boolean includeClientSecret) {
        if (p == null) {
            return null;
        }
        PaymentDto dto = new PaymentDto();
        dto.setId(p.getId());
        dto.setOrderId(p.getOrder() != null ? p.getOrder().getId() : null);
        dto.setMethod(p.getMethod());
        dto.setProvider(p.getProvider());
        dto.setStatus(p.getStatus());
        dto.setAmount(p.getAmount());
        dto.setRefundedAmount(p.getRefundedAmount());
        dto.setRefundableAmount(p.getRefundableAmount());
        dto.setCurrency(p.getCurrency());
        dto.setProviderPaymentId(p.getProviderPaymentId());
        if (includeClientSecret && p.getStatus() == com.hellfire.model.PaymentStatus.PENDING
                && p.getProvider() != null && p.getProvider().isGateway()) {
            dto.setClientSecret(p.getProviderClientSecret());
        }
        dto.setPaidAt(p.getPaidAt());
        dto.setFailureReason(p.getFailureReason());
        dto.setHasOpenRefund(p.hasOpenRefund());
        dto.setCommissionPercentage(p.getCommissionPercentage());
        dto.setCommissionAmount(p.getCommissionAmount());
        return dto;
    }

    public static RefundDto toDto(Refund r, boolean revealBank) {
        Payment p = r.getPayment();
        RefundDto.RefundDtoBuilder b = RefundDto.builder()
                .id(r.getId())
                .paymentId(p != null ? p.getId() : null)
                .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
                .amount(r.getAmount())
                .currency(p != null ? p.getCurrency() : null)
                .reason(r.getReason())
                .status(r.getStatus())
                .method(r.getMethod())
                .providerRefundId(r.getProviderRefundId())
                .requestedBy(r.getRequestedBy())
                .requestedByCustomer(r.isRequestedByCustomer())
                .requestedAt(r.getRequestedAt())
                .processedBy(r.getProcessedBy())
                .processedAt(r.getProcessedAt())
                .referenceNumber(r.getReferenceNumber())
                .notes(r.getNotes())
                .rejectionReason(r.getRejectionReason());
        if (p != null) {
            if (p.getRestaurant() != null) {
                b.restaurantId(p.getRestaurant().getId()).restaurantName(p.getRestaurant().getName());
            }
            if (p.getCustomer() != null) {
                b.customerId(p.getCustomer().getId())
                        .customerName(p.getCustomer().getFullName())
                        .customerEmail(p.getCustomer().getEmail());
            }
        }
        if (r.getBeneficiaryName() != null || r.getAccountNumber() != null || r.getUpiId() != null) {
            b.bankAccount(new BankAccountDto(
                    r.getBeneficiaryName(),
                    revealBank ? r.getAccountNumber() : BankDetails.maskAccountNumber(r.getAccountNumber()),
                    r.getIfsc(),
                    null,
                    revealBank ? r.getUpiId() : BankDetails.maskUpi(r.getUpiId()),
                    !revealBank,
                    null,
                    null));
        }
        return b.build();
    }

    public static PayoutDto toDto(Payout p, boolean revealBank, List<Payment> payments) {
        PayoutDto.PayoutDtoBuilder b = PayoutDto.builder()
                .id(p.getId())
                .restaurantId(p.getRestaurant() != null ? p.getRestaurant().getId() : null)
                .restaurantName(p.getRestaurant() != null ? p.getRestaurant().getName() : null)
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .paymentCount(p.getPaymentCount())
                .grossAmount(p.getGrossAmount())
                .refundedAmount(p.getRefundedAmount())
                .commissionPercentage(p.getCommissionPercentage())
                .commissionAmount(p.getCommissionAmount())
                .netAmount(p.getNetAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .createdBy(p.getCreatedBy())
                .paidAt(p.getPaidAt())
                .paidBy(p.getPaidBy())
                .referenceNumber(p.getReferenceNumber())
                .notes(p.getNotes());
        if (p.getAccountHolderName() != null || p.getAccountNumber() != null) {
            b.bankAccount(new BankAccountDto(
                    p.getAccountHolderName(),
                    revealBank ? p.getAccountNumber() : BankDetails.maskAccountNumber(p.getAccountNumber()),
                    p.getIfsc(),
                    p.getBankName(),
                    null,
                    !revealBank,
                    null,
                    null));
        }
        if (payments != null) {
            b.payments(payments.stream().map(pay -> toDto(pay, false)).toList());
        }
        return b.build();
    }
}
