package com.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating/updating an inventory item.
 *
 * @param sku            unique product identifier (normalised to uppercase)
 * @param productId      id of the owning product in product-service
 * @param productName    display name snapshot
 * @param availableQuantity initial stock available to promise
 * @param reorderLevel   threshold below which the item is considered low-stock
 */
public record InventoryItemRequest(
        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must be at most 64 characters")
        String sku,

        @NotNull(message = "productId is required")
        Long productId,

        @NotBlank(message = "productName is required")
        @Size(max = 255, message = "productName must be at most 255 characters")
        String productName,

        @Min(value = 0, message = "availableQuantity must be zero or greater")
        int availableQuantity,

        @Min(value = 0, message = "reorderLevel must be zero or greater")
        int reorderLevel
) {
}
