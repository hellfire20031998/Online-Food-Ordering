package com.hellfire.payment.gateway;

/** Result of creating a payment attempt: the provider id and the secret the browser uses to complete it. */
public record GatewayIntent(String providerPaymentId, String clientSecret) {
}
