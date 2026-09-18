package com.hellfire.model;

import com.hellfire.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The money side of an order: one row per order. Records how it was (or will be) paid, what the
 * platform keeps, how much has been refunded, and which payout settled it to the restaurant.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @ToString.Exclude
    private Order order;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private Restaurant restaurant;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private User customer;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentMethods method;

    @Enumerated(EnumType.STRING)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** Gateway object id, e.g. a Stripe PaymentIntent id. */
    private String providerPaymentId;

    /** Needed to resume checkout for an unpaid order; never returned to anyone but the customer. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String providerClientSecret;

    /** Platform commission snapshot taken when the payment settled. */
    @Column(precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @Column(length = 500)
    private String failureReason;

    /** Set once the money has been included in a restaurant payout. */
    @ManyToOne
    @ToString.Exclude
    private Payout payout;

    @OneToMany(mappedBy = "payment")
    @ToString.Exclude
    private List<Refund> refunds = new ArrayList<>();

    // ------------------------------------------------------------------ derived amounts

    public BigDecimal getRefundableAmount() {
        if (status == null || !status.isSettled()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(refundedAmount == null ? BigDecimal.ZERO : refundedAmount).max(BigDecimal.ZERO);
    }

    /** Amount actually kept after refunds. */
    public BigDecimal getSettledAmount() {
        return amount.subtract(refundedAmount == null ? BigDecimal.ZERO : refundedAmount).max(BigDecimal.ZERO);
    }

    /** Commission on the settled amount at the snapshot percentage. */
    public BigDecimal getCommissionOnSettled() {
        if (commissionPercentage == null) {
            return BigDecimal.ZERO;
        }
        return getSettledAmount().multiply(commissionPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** What the restaurant is owed for this payment. */
    public BigDecimal getNetToRestaurant() {
        return getSettledAmount().subtract(getCommissionOnSettled());
    }

    public boolean hasOpenRefund() {
        return refunds != null && refunds.stream().anyMatch(r -> r.getStatus() != null && r.getStatus().isOpen());
    }
}
