package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerEmail,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        ShippingAddressResponse shippingAddress,
        String notes,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCurrency(),
                ShippingAddressResponse.from(order.getShippingAddress()),
                order.getNotes(),
                order.getCancelReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}