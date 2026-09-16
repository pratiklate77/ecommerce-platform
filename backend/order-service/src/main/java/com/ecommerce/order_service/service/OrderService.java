package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CancelOrderRequest;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderItemRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.OrderStatusUpdateRequest;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.model.ShippingAddress;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orchestrates the order lifecycle: creation, customer-scoped reads, status
 * transitions and cancellation. Prices are computed server-side and broker
 * events are published only after the change is committed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderPriceCalculator priceCalculator;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse create(Long customerId, String customerEmail, OrderCreateRequest request) {
        OrderPriceCalculator.PriceBreakdown breakdown =
                priceCalculator.calculate(request.items(), request.shippingCost(), request.taxAmount());

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.next())
                .customerId(customerId)
                .customerEmail(customerEmail)
                .status(OrderStatus.PENDING)
                .subtotal(breakdown.subtotal())
                .shippingCost(breakdown.shippingCost())
                .taxAmount(breakdown.taxAmount())
                .totalAmount(breakdown.totalAmount())
                .currency(request.currency().toUpperCase())
                .shippingAddress(toShippingAddress(request))
                .notes(request.notes())
                .build();

        request.items().forEach(item -> order.addItem(toOrderItem(item)));

        Order saved = orderRepository.save(order);
        eventPublisher.orderCreated(saved);
        log.info("Placed order {} for customer {}", saved.getOrderNumber(), customerId);

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long customerId, Long id) {
        return OrderResponse.from(findByIdAndCustomerIdOrThrow(customerId, id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForCustomer(Long customerId, OrderStatus status) {
        List<Order> orders = (status == null)
                ? orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                : orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, status);
        return orders.stream().map(OrderResponse::from).toList();
    }

    /**
     * Advances an order's status (admin-driven). Uses the transition table in
     * {@link OrderStatus}; illegal transitions are rejected.
     */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = findByIdOrThrow(id);
        OrderStatus target = request.status();
        OrderStatus current = order.getStatus();

        if (target == current) {
            throw new ConflictException("Order " + order.getOrderNumber() + " is already " + target);
        }
        if (!current.canTransitionTo(target)) {
            throw new ConflictException("Cannot move order " + order.getOrderNumber()
                    + " from " + current + " to " + target);
        }

        order.setStatus(target);
        order.setCancelReason(request.reason());
        eventPublisher.statusChanged(order, current.name(), target.name(), request.reason());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(Long customerId, Long id, CancelOrderRequest request) {
        Order order = findByIdAndCustomerIdOrThrow(customerId, id);

        if (!order.getStatus().isCancelable()) {
            throw new ConflictException("Order " + order.getOrderNumber()
                    + " cannot be cancelled in state " + order.getStatus());
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.reason());
        eventPublisher.orderCancelled(order, previous, request.reason());
        log.info("Cancelled order {} for customer {}", order.getOrderNumber(), customerId);
        return OrderResponse.from(order);
    }

    /**
     * Driven by the {@code order.paid} event consumed from payment-service.
     * Idempotent: already-confirmed orders are left untouched.
     */
    @Transactional
    public void markPaid(Long id, String paymentId) {
        Order order = findByIdOrThrow(id);
        OrderStatus current = order.getStatus();

        if (current == OrderStatus.CONFIRMED) {
            log.debug("Order {} already confirmed (paymentId {})", order.getOrderNumber(), paymentId);
            return;
        }
        if (!current.canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new ConflictException("Cannot confirm order " + order.getOrderNumber()
                    + " from state " + current);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        eventPublisher.statusChanged(order, current.name(), OrderStatus.CONFIRMED.name(), "paymentId=" + paymentId);
        log.info("Order {} confirmed after payment {}", order.getOrderNumber(), paymentId);
    }

    private OrderItem toOrderItem(OrderItemRequest item) {
        return OrderItem.builder()
                .productId(item.productId())
                .productName(item.productName())
                .sku(item.sku().trim().toUpperCase())
                .unitPrice(item.unitPrice())
                .quantity(item.quantity())
                .lineTotal(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .build();
    }

    private ShippingAddress toShippingAddress(OrderCreateRequest request) {
        if (request.shippingAddress() == null) {
            return null;
        }
        var addr = request.shippingAddress();
        return ShippingAddress.builder()
                .line1(addr.line1())
                .line2(addr.line2())
                .city(addr.city())
                .state(addr.state())
                .postalCode(addr.postalCode())
                .country(addr.country())
                .build();
    }

    private Order findByIdAndCustomerIdOrThrow(Long customerId, Long id) {
        return orderRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order findByIdOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}