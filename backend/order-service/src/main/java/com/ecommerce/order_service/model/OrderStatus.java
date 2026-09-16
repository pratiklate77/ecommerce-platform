package com.ecommerce.order_service.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states an {@link Order} can be in. Transition rules are centralised
 * here so neither the service layer nor callers can drift out of sync.
 */
public enum OrderStatus {

    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED;

    /** States from which the customer may cancel the order. */
    private static final Set<OrderStatus> CANCELABLE = EnumSet.of(PENDING, CONFIRMED, PROCESSING);

    public boolean isCancelable() {
        return CANCELABLE.contains(this);
    }

    /** Returns {@code true} if a direct transition from {@code this} to {@code target} is allowed. */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTargets(this).contains(target);
    }

    private static Set<OrderStatus> allowedTargets(OrderStatus from) {
        return switch (from) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED, FAILED);
            case CONFIRMED -> EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED, CANCELLED);
            // Terminal states have no legal outgoing transitions.
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
            case FAILED -> EnumSet.of(PENDING, CANCELLED);
        };
    }
}