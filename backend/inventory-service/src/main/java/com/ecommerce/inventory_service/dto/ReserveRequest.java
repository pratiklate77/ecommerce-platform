package com.ecommerce.inventory_service.dto;

import java.util.List;

/**
 * Manually reserve stock for an order (primarily for testing/admin). The
 * event-driven path used by {@code order.created} does the same underneath.
 */
public record ReserveRequest(
        Long orderId,
        List<ReserveItem> items
) {
    public record ReserveItem(
            String sku,
            int quantity
    ) {
    }
}
