package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderPriceCalculatorTest {

    private final OrderPriceCalculator calculator = new OrderPriceCalculator();

    @Test
    void sumsLineTotalsForTheSubtotal() {
        var breakdown = calculator.calculate(
                List.of(
                        item("unitPrice", new BigDecimal("10.00"), 2),
                        item("unitPrice", new BigDecimal("5.50"), 1)),
                new BigDecimal("4.00"),
                new BigDecimal("2.00"));

        assertEquals(new BigDecimal("25.50"), breakdown.subtotal());
        assertEquals(new BigDecimal("4.00"), breakdown.shippingCost());
        assertEquals(new BigDecimal("2.00"), breakdown.taxAmount());
        assertEquals(new BigDecimal("31.50"), breakdown.totalAmount());
    }

    @Test
    void treatsMissingShippingAndTaxAsZero() {
        var breakdown = calculator.calculate(
                List.of(item("unitPrice", new BigDecimal("20.00"), 1)),
                null,
                null);

        assertEquals(new BigDecimal("20.00"), breakdown.totalAmount());
    }

    private OrderItemRequest item(String name, BigDecimal price, int qty) {
        return new OrderItemRequest(1L, name, "SKU", price, qty);
    }
}