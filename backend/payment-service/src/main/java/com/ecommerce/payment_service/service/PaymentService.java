package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentCreateRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.exception.ResourceNotFoundException;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates payments for orders: creates a payment and verifies it through the
 * simulated gateway (the "dummy" payment service, {@link PaymentGatewaySimulator}).
 * Also handles refunds, both ad-hoc and when an order is cancelled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator gateway;

    /**
     * Creates and verifies a payment for an order. Idempotent for an order that
     * already has a VERIFIED payment (returns it unchanged).
     */
    @Transactional
    public PaymentResponse createAndVerify(Long customerId, String customerEmail, PaymentCreateRequest request) {
        Optional<Payment> existing = paymentRepository.findFirstByOrderIdAndStatus(
                request.orderId(), PaymentStatus.VERIFIED);
        if (existing.isPresent()) {
            log.info("Order {} already has a verified payment {}", request.orderNumber(), existing.get().getPaymentId());
            return PaymentResponse.from(existing.get());
        }

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID().toString())
                .orderId(request.orderId())
                .orderNumber(request.orderNumber())
                .customerId(customerId)
                .customerEmail(customerEmail)
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .build();

        try {
            String txRef = gateway.charge(request.paymentToken(), request.amount());
            payment.setTransactionRef(txRef);
            payment.setStatus(PaymentStatus.VERIFIED);
        } catch (PaymentGatewaySimulator.PaymentDeclinedException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);
            log.warn("Payment {} failed for order {}: {}", payment.getPaymentId(), request.orderNumber(), ex.getMessage());
            return PaymentResponse.from(payment);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Verified payment {} for order {}", saved.getPaymentId(), saved.getOrderNumber());
        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByPaymentId(String paymentId) {
        return PaymentResponse.from(findByPaymentIdOrThrow(paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * Refunds a verified (non-refunded) payment by id. Idempotent for refunds
     * already applied.
     */
    @Transactional
    public PaymentResponse refund(String paymentId) {
        Payment payment = findByPaymentIdOrThrow(paymentId);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.debug("Payment {} already refunded", paymentId);
            return PaymentResponse.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.VERIFIED) {
            throw new ConflictException("Only a verified payment can be refunded; got " + payment.getStatus());
        }
        gateway.refund(payment.getTransactionRef());
        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Refunded payment {} for order {}", payment.getPaymentId(), payment.getOrderNumber());
        return PaymentResponse.from(payment);
    }

    private Payment findByPaymentIdOrThrow(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }
}
