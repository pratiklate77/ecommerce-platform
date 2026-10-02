package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentId,
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        BigDecimal amount,
        String currency,
        String method,
        PaymentStatus status,
        String transactionRef,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getOrderNumber(),
                payment.getCustomerId(),
                payment.getCustomerEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getTransactionRef(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
