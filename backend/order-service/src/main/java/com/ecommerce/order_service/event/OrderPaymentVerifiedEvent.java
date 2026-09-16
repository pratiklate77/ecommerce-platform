package com.ecommerce.order_service.event;

/**
 * Consumed from payment-service after a payment for an order has been verified.
 * OrdService transitions the order from PENDING to CONFIRMED in response.
 */
public record OrderPaymentVerifiedEvent(
        Long orderId,
        String orderNumber,
        String paymentId
) {
}