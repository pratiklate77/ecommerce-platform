package com.ecommerce.payment_service.controller;

import com.ecommerce.payment_service.dto.PaymentCreateRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.exception.UnauthorizedException;
import com.ecommerce.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Payment-facing endpoints. The customer identity is resolved from
 * gateway-forwarded {@code X-User-Id} rather than trusted from the body.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createAndVerify(@Valid @RequestBody PaymentCreateRequest body,
                                                           HttpServletRequest request) {
        PaymentResponse response = paymentService.createAndVerify(userId(request), userEmail(request), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable String paymentId) {
        return paymentService.getByPaymentId(paymentId);
    }

    @GetMapping
    public List<PaymentResponse> listForOrder(@RequestParam Long orderId) {
        return paymentService.listForOrder(orderId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponse refund(@PathVariable String paymentId) {
        return paymentService.refund(paymentId);
    }

    private Long userId(HttpServletRequest request) {
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Authenticated user (" + USER_ID_HEADER + " header) is required");
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException(USER_ID_HEADER + " header must be a valid number");
        }
    }

    private String userEmail(HttpServletRequest request) {
        String header = request.getHeader(USER_EMAIL_HEADER);
        return (header == null || header.isBlank()) ? null : header.trim();
    }
}
