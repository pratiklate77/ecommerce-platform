package com.ecommerce.inventory_service.dto;

import com.ecommerce.inventory_service.model.InventoryItem;

import java.time.LocalDateTime;

public record InventoryItemResponse(
        Long id,
        String sku,
        Long productId,
        String productName,
        int availableQuantity,
        int reservedQuantity,
        int reorderLevel,
        int totalOnHand,
        boolean active,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getSku(),
                item.getProductId(),
                item.getProductName(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getReorderLevel(),
                item.totalOnHand(),
                item.isActive(),
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
