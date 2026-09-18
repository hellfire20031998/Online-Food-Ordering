package com.hellfire.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Restaurant owner's view of their money. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarningsSummaryDto {

    private String currency;
    /** Settled payments not yet included in a payout: what the next payout will roughly contain. */
    private int unsettledPayments;
    private BigDecimal unsettledGross;
    private BigDecimal unsettledCommission;
    private BigDecimal unsettledNet;
    /** Payouts generated but not yet transferred. */
    private BigDecimal pendingPayouts;
    /** Everything transferred so far. */
    private BigDecimal paidOut;
    private BigDecimal commissionPercentage;
}
