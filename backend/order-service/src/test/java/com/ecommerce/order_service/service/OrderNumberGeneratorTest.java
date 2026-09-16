package com.ecommerce.order_service.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNumberGeneratorTest {

    private final OrderNumberGenerator generator = new OrderNumberGenerator();

    @Test
    void generatesOrderNumberWithOrderPrefix() {
        assertTrue(generator.next().startsWith("ORD-"));
    }

    @Test
    void generatesDistinctOrderNumbers() {
        assertTrue(!generator.next().equals(generator.next()));
    }
}