package com.hellfire.model;

import com.hellfire.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Money returned to a customer. Gateway payments are reversed through the gateway; cash-on-delivery
 * orders are refunded by manual bank transfer to details the customer provides.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private Payment payment;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private Order order;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    @Enumerated(EnumType.STRING)
    private RefundMethod method;

    private String providerRefundId;

    private String requestedBy;
    private boolean requestedByCustomer;
    private LocalDateTime requestedAt;

    private String processedBy;
    private LocalDateTime processedAt;

    /** Bank transaction reference for manual refunds. */
    private String referenceNumber;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String rejectionReason;

    // ---- customer's bank details for BANK_TRANSFER refunds
    private String beneficiaryName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String accountNumber;

    private String ifsc;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String upiId;

    public boolean hasBankDetails() {
        return beneficiaryName != null && !beneficiaryName.isBlank()
                && ((accountNumber != null && !accountNumber.isBlank() && ifsc != null && !ifsc.isBlank())
                || (upiId != null && !upiId.isBlank()));
    }
}
