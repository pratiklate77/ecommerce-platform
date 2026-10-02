package com.ecommerce.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to create a payment for an order. The customer identity is taken from
 * gateway-forwarded headers, not this body.
 *
 * @param orderId    the order being paid for
 * @param orderNumber human-readable number for the event/refund trail
 * @param amount     the amount to charge (must be positive)
 * @param currency   ISO-4217 code
 * @param method     payment method label (e.g. CARD, PAYPAL)
 * @param paymentToken opaque gateway token/card ref; include "decline" to simulate a rejection
 */
public record PaymentCreateRequest(
        @NotNull(message = "orderId is required")
        Long orderId,

        @NotBlank(message = "orderNumber is required")
        @Size(max = 32, message = "orderNumber must be at most 32 characters")
        String orderNumber,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter code")
        String currency,

        @NotBlank(message = "method is required")
        @Size(max = 32, message = "method must be at most 32 characters")
        String method,

        @Size(max = 64, message = "paymentToken must be at most 64 characters")
        String paymentToken
) {
}
