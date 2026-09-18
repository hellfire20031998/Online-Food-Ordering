package com.hellfire.payment.gateway;

import com.hellfire.model.Payment;
import com.hellfire.model.PaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Placeholder for the Razorpay integration. Selecting {@code app.payments.gateway=razorpay} wires
 * this bean so the rest of the platform needs no change when it is implemented: Razorpay Orders map
 * to {@link #createIntent}, the payment id + signature check to {@link #fetchIntent}, Razorpay
 * Refunds to {@link #refund}, and its webhook (X-Razorpay-Signature) to {@link #parseWebhook}.
 */
@Component
@ConditionalOnProperty(name = "app.payments.gateway", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {

    private static final String NOT_YET = "Razorpay gateway is not implemented yet; use app.payments.gateway=stripe";

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.RAZORPAY;
    }

    @Override
    public String publishableKey() {
        return null;
    }

    @Override
    public GatewayIntent createIntent(Payment payment) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public GatewayIntentStatus fetchIntent(String providerPaymentId) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public void cancelIntent(String providerPaymentId) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public GatewayRefundResult refund(String providerPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public GatewayWebhookEvent parseWebhook(String payload, String signatureHeader) {
        throw new UnsupportedOperationException(NOT_YET);
    }
}
