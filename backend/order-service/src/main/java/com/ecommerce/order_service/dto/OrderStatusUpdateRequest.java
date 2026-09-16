package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin-driven status transition. The transition itself is validated against the
 * {@link OrderStatus} transition table in the service layer.
 */
public record OrderStatusUpdateRequest(
        @NotNull(message = "status is required")
        OrderStatus status,

        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason
) {
}