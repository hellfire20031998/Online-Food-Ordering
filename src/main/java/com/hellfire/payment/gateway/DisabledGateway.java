package com.hellfire.payment.gateway;

import com.hellfire.model.Payment;
import com.hellfire.model.PaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Default when no gateway is configured: only cash on delivery is possible. */
@Component
@ConditionalOnProperty(name = "app.payments.gateway", havingValue = "none", matchIfMissing = true)
public class DisabledGateway implements PaymentGateway {

    private static final String MESSAGE = "Online payments are not available right now. Please choose cash on delivery.";

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public PaymentProvider provider() {
        return null;
    }

    @Override
    public String publishableKey() {
        return null;
    }

    @Override
    public GatewayIntent createIntent(Payment payment) {
        throw new IllegalArgumentException(MESSAGE);
    }

    @Override
    public GatewayIntentStatus fetchIntent(String providerPaymentId) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public void cancelIntent(String providerPaymentId) {
        // nothing to cancel
    }

    @Override
    public GatewayRefundResult refund(String providerPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        throw new IllegalStateException("No payment gateway is configured; refund this order by bank transfer instead.");
    }

    @Override
    public GatewayWebhookEvent parseWebhook(String payload, String signatureHeader) {
        throw new IllegalArgumentException("No payment gateway is configured");
    }
}
