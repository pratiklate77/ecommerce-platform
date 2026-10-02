package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentCreateRequest;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewaySimulator gateway;

    private Payment verifiedPayment() {
        return Payment.builder()
                .id(1L).paymentId("p-1").orderId(10L).orderNumber("ORD-1").customerId(1L)
                .customerEmail("a@b.com").amount(new BigDecimal("50.00")).currency("USD")
                .method("CARD").transactionRef("TX_123").status(PaymentStatus.VERIFIED).build();
    }

    private PaymentCreateRequest request(String token) {
        return new PaymentCreateRequest(10L, "ORD-1", new BigDecimal("50.00"), "USD", "CARD", token);
    }

    @Test
    void createAndVerifyApprovedReturnsVerified() {
        when(paymentRepository.findFirstByOrderIdAndStatus(10L, PaymentStatus.VERIFIED))
                .thenReturn(Optional.empty());
        when(gateway.charge(any(), any())).thenReturn("TX_ABC");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = paymentService.createAndVerify(1L, "a@b.com", request("tok_ok"));

        assertEquals(PaymentStatus.VERIFIED, response.status());
        assertEquals("TX_ABC", response.transactionRef());
    }

    @Test
    void createAndVerifyDeclinedMarksFailed() {
        when(paymentRepository.findFirstByOrderIdAndStatus(10L, PaymentStatus.VERIFIED))
                .thenReturn(Optional.empty());
        when(gateway.charge(any(), any()))
                .thenThrow(new PaymentGatewaySimulator.PaymentDeclinedException("Gateway declined"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = paymentService.createAndVerify(1L, "a@b.com", request("tok_decline"));

        assertEquals(PaymentStatus.FAILED, response.status());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createAndVerifyIsIdempotentForExistingVerifiedPayment() {
        when(paymentRepository.findFirstByOrderIdAndStatus(10L, PaymentStatus.VERIFIED))
                .thenReturn(Optional.of(verifiedPayment()));

        var response = paymentService.createAndVerify(1L, "a@b.com", request("tok_ok"));

        assertEquals("p-1", response.paymentId());
        verify(gateway, never()).charge(any(), any());
    }

    @Test
    void refundVerifiedPaymentMarksRefunded() {
        Payment payment = verifiedPayment();
        when(paymentRepository.findByPaymentId("p-1")).thenReturn(Optional.of(payment));

        var response = paymentService.refund("p-1");

        assertEquals(PaymentStatus.REFUNDED, response.status());
        verify(gateway).refund("TX_123");
    }

    @Test
    void refundRejectsNonVerifiedPayment() {
        Payment payment = verifiedPayment();
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByPaymentId("p-1")).thenReturn(Optional.of(payment));

        assertThrows(ConflictException.class, () -> paymentService.refund("p-1"));
    }
}
