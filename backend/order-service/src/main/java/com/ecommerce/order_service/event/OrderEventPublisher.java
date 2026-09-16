package com.ecommerce.order_service.event;

import com.ecommerce.order_service.config.KafkaTopics;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link KafkaTemplate} that exposes intent-revealing
 * methods for the order lifecycle. Keeping the template behind one place makes
 * event serialisation and topic naming consistent and easy to test.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void orderCreated(Order order) {
        publish(KafkaTopics.ORDER_CREATED, order.getOrderNumber(), OrderCreatedEvent.from(order));
    }

    public void orderCancelled(Order order, OrderStatus previousStatus, String reason) {
        publish(KafkaTopics.ORDER_CANCELLED, order.getOrderNumber(),
                new OrderStatusChangedEvent(
                        order.getId(), order.getOrderNumber(),
                        previousStatus.name(), order.getStatus().name(), reason));
    }

    public void statusChanged(Order order, String from, String to, String reason) {
        publish(KafkaTopics.ORDER_STATUS_CHANGED, order.getOrderNumber(),
                new OrderStatusChangedEvent(order.getId(), order.getOrderNumber(), from, to, reason));
    }

    private void publish(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload);
        log.debug("Published event to topic '{}' with key '{}'", topic, key);
    }
}