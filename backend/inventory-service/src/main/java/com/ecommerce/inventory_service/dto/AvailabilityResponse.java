package com.ecommerce.inventory_service.dto;

/**
 * Quick availability read for a product, used by the storefront/gateway.
 */
public record AvailabilityResponse(
        String sku,
        int availableQuantity,
        int reservedQuantity,
        boolean inStock
) {
}
