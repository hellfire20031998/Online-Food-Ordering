package com.hellfire.payment.gateway;

/** @param completed true when the provider already confirmed the refund, false when it is still processing. */
public record GatewayRefundResult(String providerRefundId, boolean completed) {
}
