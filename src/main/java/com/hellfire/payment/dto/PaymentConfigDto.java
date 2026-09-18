package com.hellfire.payment.dto;

import com.hellfire.model.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What the checkout page needs to know before offering payment methods. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfigDto {

    private boolean onlinePaymentsEnabled;
    private PaymentProvider provider;
    private String publishableKey;
    private String currency;
}
