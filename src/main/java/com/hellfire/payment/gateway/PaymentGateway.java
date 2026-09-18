package com.hellfire.payment.gateway;

import com.hellfire.model.Payment;
import com.hellfire.model.PaymentProvider;

import java.math.BigDecimal;

/**
 * Online payment provider abstraction. Exactly one implementation is active, selected by
 * {@code app.payments.gateway} ({@code none} by default, {@code stripe}, later {@code razorpay}).
 * <p>
 * The contract is intentionally small: create a payment attempt the browser can complete,
 * read its state back, cancel it, refund it, and translate the provider's webhook into a
 * {@link GatewayWebhookEvent}. Everything else (order state, ledgers, emails) lives in
 * {@code PaymentService} and is provider-independent.
 */
public interface PaymentGateway {

    /** False for the default no-op gateway; online payment methods are then refused at checkout. */
    boolean enabled();

    PaymentProvider provider();

    /** Key the browser SDK needs (Stripe publishable key, Razorpay key id); null when disabled. */
    String publishableKey();

    /** Creates the provider-side payment attempt for this (already persisted) payment. */
    GatewayIntent createIntent(Payment payment);

    GatewayIntentStatus fetchIntent(String providerPaymentId);

    void cancelIntent(String providerPaymentId);

    GatewayRefundResult refund(String providerPaymentId, BigDecimal amount, String reason, String idempotencyKey);

    /** Verifies the signature and normalises the event. Throws IllegalArgumentException when invalid. */
    GatewayWebhookEvent parseWebhook(String payload, String signatureHeader);
}
