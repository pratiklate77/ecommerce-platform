package com.ecommerce.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload for creating a new order. The authenticated customer identity is not
 * part of the body; it is taken from gateway-forwarded headers by the controller,
 * which keeps the client from impersonating arbitrary users.
 *
 * @param currency       ISO-4217 three-letter code for the whole order
 * @param shippingAddress delivery snapshot
 * @param notes          free-form note attached to the order
 * @param items          at least one line item
 * @param shippingCost   positive shipping fee, 0 when absent
 * @param taxAmount      positive tax, 0 when absent
 */
public record OrderCreateRequest(
        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter code (e.g. USD)")
        String currency,

        @Valid
        ShippingAddressRequest shippingAddress,

        @Size(max = 1000, message = "notes must be at most 1000 characters")
        String notes,

        @NotEmpty(message = "an order must contain at least one item")
        @Valid
        List<@Valid OrderItemRequest> items,

        @DecimalMin(value = "0.00", message = "shippingCost must be zero or greater")
        BigDecimal shippingCost,

        @DecimalMin(value = "0.00", message = "taxAmount must be zero or greater")
        BigDecimal taxAmount
) {
}

