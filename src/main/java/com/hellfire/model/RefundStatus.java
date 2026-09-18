package com.hellfire.model;

public enum RefundStatus {
    /** Raised by a customer or on cancellation; awaiting a team decision. */
    REQUESTED,
    /** Approved for a manual bank transfer; awaiting the transfer reference. */
    APPROVED,
    /** Sent to the gateway; awaiting its confirmation. */
    PROCESSING,
    COMPLETED,
    REJECTED,
    FAILED;

    public boolean isOpen() {
        return this == REQUESTED || this == APPROVED || this == PROCESSING;
    }
}
