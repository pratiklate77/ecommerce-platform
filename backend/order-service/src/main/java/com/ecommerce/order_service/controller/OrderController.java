package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.CancelOrderRequest;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.OrderStatusUpdateRequest;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.security.UserPrincipalRequest;
import com.ecommerce.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Order-facing endpoints. The caller identity comes from the authenticated
 * {@link UserPrincipalRequest}, populated from the signed JWT by
 * {@code JwtAuthenticationFilter}. The service is secured as a whole
 * (see {@code SecurityConfig}), so controllers never touch {@code
 * HttpServletRequest} or parse headers themselves.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal UserPrincipalRequest principal,
            @Valid @RequestBody OrderCreateRequest body) {
        OrderResponse created = orderService.create(
                principal.getUserId(), principal.getEmail(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<OrderResponse> list(
            @AuthenticationPrincipal UserPrincipalRequest principal,
            @RequestParam(required = false) OrderStatus status) {
        return orderService.listForCustomer(principal.getUserId(), status);
    }

    @GetMapping("/{id}")
    public OrderResponse get(
            @AuthenticationPrincipal UserPrincipalRequest principal,
            @PathVariable Long id) {
        return orderService.get(principal.getUserId(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
            @AuthenticationPrincipal UserPrincipalRequest principal,
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest body) {
        return orderService.cancel(principal.getUserId(), id, body);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody OrderStatusUpdateRequest body) {
        return orderService.updateStatus(id, body);
    }
}
