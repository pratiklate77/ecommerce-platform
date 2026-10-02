package com.ecommerce.payment_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stand-in for an external payment gateway (Stripe/Adyen/...). In a real system
 * this would issue an HTTP call to the provider and handle webhooks; here it
 * always approves well-formed charges and returns a stable transaction ref.
 *
 * You can force a failure by passing a card/token whose reference contains the
 * literal string {@code decline}, which is handy for exercising refunds and the
 * FAILED state in local testing.
 */
@Slf4j
@Component
public class PaymentGatewaySimulator {

    /**
     * @param tokenRef  opaque payment token/card reference provided at checkout
     * @param amount    charge amount
     * @return the gateway transaction reference on success
     * @throws PaymentDeclinedException if the provider rejects the charge
     */
    public String charge(String tokenRef, BigDecimal amount) {
        if (tokenRef != null && tokenRef.toLowerCase().contains("decline")) {
            throw new PaymentDeclinedException("Gateway declined charge for token " + tokenRef);
        }
        String txRef = "TX_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Simulated gateway approved charge {} for amount {}", txRef, amount);
        return txRef;
    }

    /** Idempotent refund acknowledgement; always succeeds for known transactions. */
    public void refund(String transactionRef) {
        log.info("Simulated gateway refunded transaction {}", transactionRef);
    }

    public static class PaymentDeclinedException extends RuntimeException {
        public PaymentDeclinedException(String message) {
            super(message);
        }
    }
}
