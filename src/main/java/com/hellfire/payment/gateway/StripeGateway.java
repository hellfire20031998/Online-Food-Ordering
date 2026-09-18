package com.hellfire.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.model.Payment;
import com.hellfire.model.PaymentProvider;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Stripe implementation using PaymentIntents (card, UPI, net banking and wallets are all enabled
 * through Stripe's automatic payment methods). Amounts are sent in the currency's minor unit (paise).
 */
@Component
@ConditionalOnProperty(name = "app.payments.gateway", havingValue = "stripe")
public class StripeGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String secretKey;
    private final RequestOptions options;
    private final String publishableKey;
    private final String webhookSecret;

    public StripeGateway(@Value("${app.payments.stripe.secret-key}") String secretKey,
                         @Value("${app.payments.stripe.publishable-key}") String publishableKey,
                         @Value("${app.payments.stripe.webhook-secret:}") String webhookSecret) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY must be set when app.payments.gateway=stripe");
        }
        this.secretKey = secretKey;
        this.options = RequestOptions.builder().setApiKey(secretKey).build();
        this.publishableKey = publishableKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public String publishableKey() {
        return publishableKey;
    }

    @Override
    public GatewayIntent createIntent(Payment payment) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(payment.getAmount()))
                    .setCurrency(payment.getCurrency().toLowerCase())
                    .setDescription("Order #" + payment.getOrder().getId())
                    .putMetadata("orderId", String.valueOf(payment.getOrder().getId()))
                    .putMetadata("paymentId", String.valueOf(payment.getId()))
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .build();
            PaymentIntent intent = PaymentIntent.create(params, options);
            return new GatewayIntent(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe could not start the payment: " + e.getMessage(), e);
        }
    }

    @Override
    public GatewayIntentStatus fetchIntent(String providerPaymentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerPaymentId, options);
            String reason = intent.getLastPaymentError() != null ? intent.getLastPaymentError().getMessage() : null;
            return new GatewayIntentStatus(mapIntentStatus(intent.getStatus()), reason);
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe lookup failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelIntent(String providerPaymentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerPaymentId, options);
            if (!"succeeded".equals(intent.getStatus()) && !"canceled".equals(intent.getStatus())) {
                intent.cancel(PaymentIntentCancelParams.builder().build(), options);
            }
        } catch (StripeException e) {
            log.warn("Could not cancel Stripe PaymentIntent {}: {}", providerPaymentId, e.getMessage());
        }
    }

    @Override
    public GatewayRefundResult refund(String providerPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(providerPaymentId)
                    .setAmount(toMinorUnits(amount))
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .putMetadata("reason", reason == null ? "" : reason)
                    .build();
            RequestOptions refundOptions = RequestOptions.builder()
                    .setApiKey(secretKey)
                    .setIdempotencyKey(idempotencyKey)
                    .build();
            Refund refund = Refund.create(params, refundOptions);
            return new GatewayRefundResult(refund.getId(), "succeeded".equals(refund.getStatus()));
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public GatewayWebhookEvent parseWebhook(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalArgumentException("STRIPE_WEBHOOK_SECRET is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        JsonNode object = eventObject(event);
        String id = text(object, "id");

        switch (event.getType()) {
            case "payment_intent.succeeded":
                return new GatewayWebhookEvent(GatewayWebhookEvent.Type.PAYMENT_SUCCEEDED, id, null, null);
            case "payment_intent.payment_failed":
                return new GatewayWebhookEvent(GatewayWebhookEvent.Type.PAYMENT_FAILED, id, null,
                        Optional.ofNullable(text(object.path("last_payment_error"), "message")).orElse("Payment failed"));
            case "payment_intent.canceled":
                return new GatewayWebhookEvent(GatewayWebhookEvent.Type.PAYMENT_CANCELLED, id, null, null);
            case "refund.updated":
            case "charge.refund.updated": {
                String status = Optional.ofNullable(text(object, "status")).orElse("");
                String paymentIntent = text(object, "payment_intent");
                if ("succeeded".equals(status)) {
                    return new GatewayWebhookEvent(GatewayWebhookEvent.Type.REFUND_COMPLETED, paymentIntent, id, null);
                }
                if ("failed".equals(status) || "canceled".equals(status)) {
                    return new GatewayWebhookEvent(GatewayWebhookEvent.Type.REFUND_FAILED, paymentIntent, id,
                            Optional.ofNullable(text(object, "failure_reason")).orElse(status));
                }
                return GatewayWebhookEvent.ignored();
            }
            default:
                return GatewayWebhookEvent.ignored();
        }
    }

    // ------------------------------------------------------------------ helpers

    static long toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    static GatewayIntentStatus.State mapIntentStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return GatewayIntentStatus.State.PENDING;
        }
        switch (stripeStatus) {
            case "succeeded":
                return GatewayIntentStatus.State.PAID;
            case "canceled":
                return GatewayIntentStatus.State.CANCELLED;
            default:
                // requires_payment_method, requires_confirmation, requires_action, processing, requires_capture
                return GatewayIntentStatus.State.PENDING;
        }
    }

    /** The typed object is unavailable when the SDK and the account's API version differ; fall back to raw JSON. */
    private static JsonNode eventObject(Event event) {
        Optional<StripeObject> typed = event.getDataObjectDeserializer().getObject();
        String raw = typed.map(StripeObject::toJson).orElseGet(() -> event.getDataObjectDeserializer().getRawJson());
        try {
            return JSON.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unreadable Stripe webhook payload");
        }
    }

    /** Text value of a field, or null when missing, null or not textual (e.g. an expanded object). */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }
}
