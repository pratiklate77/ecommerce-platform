package com.ecommerce.order_service.event;

import com.ecommerce.order_service.config.KafkaTopics;
import com.ecommerce.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes lifecycle events produced by other services. Today it handles the
 * {@code order.paid} event so confirmed payments advance PENDING orders to
 * CONFIRMED, keeping the orchestration decoupled via the broker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private final OrderService orderService;

    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentVerified(OrderPaymentVerifiedEvent event) {
        log.info("Payment verified for order {}", event.orderId());
        orderService.markPaid(event.orderId(), event.paymentId());
    }
}