package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Computes order money totals. Line totals are summed server-side rather than
 * trusting any client-supplied total, which keeps the ledger trustworthy.
 */
@Component
public class OrderPriceCalculator {

    public PriceBreakdown calculate(List<OrderItemRequest> items,
                                    BigDecimal shippingCost,
                                    BigDecimal taxAmount) {
        BigDecimal subtotal = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shipping = zeroIfNull(shippingCost);
        BigDecimal tax = zeroIfNull(taxAmount);

        return new PriceBreakdown(subtotal, shipping, tax,
                subtotal.add(shipping).add(tax));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record PriceBreakdown(
            BigDecimal subtotal,
            BigDecimal shippingCost,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {
    }
}