package com.ecommerce.inventory_service.model;

/**
 * Lifecycle of a stock {@link Reservation} for a pending order.
 */
public enum ReservationStatus {
    /** Stock is currently held for the order. */
    ACTIVE,
    /** Stock was permanently committed after the order was confirmed/paid. */
    FULFILLED,
    /** Stock was returned to available after the order was cancelled. */
    RELEASED
}
