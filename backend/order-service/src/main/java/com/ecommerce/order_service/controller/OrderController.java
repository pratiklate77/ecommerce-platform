package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.CancelOrderRequest;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.OrderStatusUpdateRequest;
import com.ecommerce.order_service.exception.UnauthorizedException;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Order-facing endpoints. The customer identity is resolved from gateway-forwarded
 * headers ({@code X-User-Id} / {@code X-User-Email}) rather than trusted from the
 * request body, so a caller cannot place orders on behalf of others.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(HttpServletRequest request,
                                                @Valid @RequestBody OrderCreateRequest body) {
        OrderResponse created = orderService.create(
                userId(request), userEmail(request), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<OrderResponse> list(HttpServletRequest request,
                                    @RequestParam(required = false) OrderStatus status) {
        return orderService.listForCustomer(userId(request), status);
    }

    @GetMapping("/{id}")
    public OrderResponse get(HttpServletRequest request, @PathVariable Long id) {
        return orderService.get(userId(request), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(HttpServletRequest request,
                                @PathVariable Long id,
                                @Valid @RequestBody CancelOrderRequest body) {
        return orderService.cancel(userId(request), id, body);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody OrderStatusUpdateRequest body) {
        return orderService.updateStatus(id, body);
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