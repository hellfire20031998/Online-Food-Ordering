package com.hellfire.payment.gateway;

/** Normalised webhook notification. Fields that do not apply to the type are null. */
public record GatewayWebhookEvent(Type type,
                                  String providerPaymentId,
                                  String providerRefundId,
                                  String failureReason) {

    public enum Type {
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        PAYMENT_CANCELLED,
        REFUND_COMPLETED,
        REFUND_FAILED,
        /** Anything the platform does not act on. */
        IGNORED
    }

    public static GatewayWebhookEvent ignored() {
        return new GatewayWebhookEvent(Type.IGNORED, null, null, null);
    }
}
