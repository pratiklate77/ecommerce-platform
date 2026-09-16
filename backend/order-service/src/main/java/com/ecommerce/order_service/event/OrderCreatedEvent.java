package com.ecommerce.order_service.event;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Emitted when an order is placed. Carries a snapshot of the order so downstream
 * consumers do not need to call back into order-service.
 */
public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getItems().stream().map(OrderItemEvent::from).toList(),
                order.getCreatedAt()
        );
    }

    public record OrderItemEvent(
            Long productId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {
        static OrderItemEvent from(OrderItem item) {
            return new OrderItemEvent(
                    item.getProductId(),
                    item.getSku(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity()
            );
        }
    }
}