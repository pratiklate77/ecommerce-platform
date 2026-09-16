package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CancelOrderRequest;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderItemRequest;
import com.ecommerce.order_service.dto.OrderStatusUpdateRequest;
import com.ecommerce.order_service.dto.ShippingAddressRequest;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @Mock
    private OrderPriceCalculator priceCalculator;

    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderCreateRequest createRequest() {
        return new OrderCreateRequest(
                "USD",
                new ShippingAddressRequest("1 Main St", null, "Springfield", "IL", "62701", "USA"),
                null,
                List.of(new OrderItemRequest(11L, "Wireless Mouse", "SKU-123", new BigDecimal("29.99"), 2)),
                new BigDecimal("5.00"),
                new BigDecimal("3.00"));
    }

    private void stubPriceBreakdown() {
        when(priceCalculator.calculate(anyList(), any(), any())).thenReturn(
                new OrderPriceCalculator.PriceBreakdown(
                        new BigDecimal("59.98"),
                        new BigDecimal("5.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("67.98")));
    }

    private Order order(Long id, Long customerId, OrderStatus status) {
        return Order.builder()
                .id(id)
                .orderNumber("ORD-1")
                .customerId(customerId)
                .customerEmail("alice@example.com")
                .status(status)
                .subtotal(new BigDecimal("59.98"))
                .shippingCost(new BigDecimal("5.00"))
                .taxAmount(new BigDecimal("3.00"))
                .totalAmount(new BigDecimal("67.98"))
                .currency("USD")
                .build();
    }

    @Test
    void createComputesTotalsAndPublishesCreatedEvent() {
        stubPriceBreakdown();
        when(orderNumberGenerator.next()).thenReturn("ORD-20260101");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var response = orderService.create(7L, "alice@example.com", createRequest());

        assertEquals(1L, response.id());
        assertEquals("ORD-20260101", response.orderNumber());
        assertEquals(7L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("67.98"), response.totalAmount());
        assertEquals(1, response.items().size());
        verify(eventPublisher).orderCreated(any(Order.class));
    }

    @Test
    void getThrowsWhenOrderDoesNotBelongToCustomer() {
        when(orderRepository.findByIdAndCustomerId(5L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.get(7L, 5L));
    }

    @Test
    void getReturnsScopedOrder() {
        when(orderRepository.findByIdAndCustomerId(5L, 7L)).thenReturn(Optional.of(order(5L, 7L, OrderStatus.PENDING)));

        var response = orderService.get(7L, 5L);

        assertEquals(5L, response.id());
    }

    @Test
    void listForCustomerFiltersByStatusWhenProvided() {
        when(orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(7L, OrderStatus.PENDING))
                .thenReturn(List.of(order(1L, 7L, OrderStatus.PENDING)));

        var results = orderService.listForCustomer(7L, OrderStatus.PENDING);

        assertEquals(1, results.size());
        verify(orderRepository).findByCustomerIdAndStatusOrderByCreatedAtDesc(7L, OrderStatus.PENDING);
    }

    @Test
    void updateStatusRejectsSameStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, 7L, OrderStatus.PENDING)));

        assertThrows(ConflictException.class, () ->
                orderService.updateStatus(1L, new OrderStatusUpdateRequest(OrderStatus.PENDING, null)));
    }

    @Test
    void updateStatusRejectsIllegalTransition() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, 7L, OrderStatus.PENDING)));

        assertThrows(ConflictException.class, () ->
                orderService.updateStatus(1L, new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null)));
    }

    @Test
    void updateStatusPublishesOnLegalTransition() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, 7L, OrderStatus.PENDING)));

        var response = orderService.updateStatus(1L, new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null));

        assertEquals(OrderStatus.CONFIRMED, response.status());
        verify(eventPublisher).statusChanged(any(Order.class), eq("PENDING"), eq("CONFIRMED"), eq(null));
    }

    @Test
    void cancelRejectsOrderInNonCancelableState() {
        when(orderRepository.findByIdAndCustomerId(1L, 7L))
                .thenReturn(Optional.of(order(1L, 7L, OrderStatus.SHIPPED)));

        assertThrows(ConflictException.class, () ->
                orderService.cancel(7L, 1L, new CancelOrderRequest("changed my mind")));
    }

    @Test
    void cancelMarksOrderCancelledAndPublishes() {
        when(orderRepository.findByIdAndCustomerId(1L, 7L))
                .thenReturn(Optional.of(order(1L, 7L, OrderStatus.PENDING)));

        var response = orderService.cancel(7L, 1L, new CancelOrderRequest("changed my mind"));

        assertEquals(OrderStatus.CANCELLED, response.status());
        verify(eventPublisher).orderCancelled(any(Order.class), eq(OrderStatus.PENDING), eq("changed my mind"));
    }

    @Test
    void markPaidConfirmsPendingOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, 7L, OrderStatus.PENDING)));

        orderService.markPaid(1L, "payment-1");

        verify(eventPublisher).statusChanged(any(Order.class), eq("PENDING"), eq("CONFIRMED"), any());
    }

    @Test
    void markPaidIsIdempotentForConfirmedOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, 7L, OrderStatus.CONFIRMED)));

        orderService.markPaid(1L, "payment-1");

        verify(eventPublisher, never()).statusChanged(any(Order.class), any(), any(), any());
    }

    @Test
    void markPaidRejectsTransitionFromTerminalState() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, 7L, OrderStatus.CANCELLED)));

        assertThrows(ConflictException.class, () -> orderService.markPaid(1L, "payment-1"));
    }

    @Test
    void markPaidThrowsWhenOrderNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.markPaid(999L, "payment-1"));
    }
}