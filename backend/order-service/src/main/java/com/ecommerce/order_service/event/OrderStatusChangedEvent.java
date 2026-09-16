package com.ecommerce.order_service.event;

/**
 * Emitted when an order moves to a new status. Downstream services use this to
 * release stock, trigger fulfilment or notify the customer.
 */
public record OrderStatusChangedEvent(
        Long orderId,
        String orderNumber,
        String from,
        String to,
        String reason
) {
}