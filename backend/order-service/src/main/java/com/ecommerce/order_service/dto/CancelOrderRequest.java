package com.ecommerce.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank(message = "a cancel reason is required")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason
) {
}