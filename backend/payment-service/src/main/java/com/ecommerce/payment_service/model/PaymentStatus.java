package com.ecommerce.payment_service.model;

/**
 * Lifecycle states of a {@link Payment}.
 */
public enum PaymentStatus {
    /** Payment created but not yet verified by the gateway. */
    PENDING,
    /** Payment authorised and verified. */
    VERIFIED,
    /** Gateway rejected the payment. */
    FAILED,
    /** A verified payment that was refunded after an order cancellation. */
    REFUNDED
}
