package com.ecommerce.order_service.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-friendly, sortable and collision-resistant order numbers of the
 * form {@code ORD-<yyyyMMddHHmmss>-<nnnn>}. The repository-level unique constraint
 * provides a final guarantee against duplicates.
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RANDOM_BOUND = 10_000;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        String suffix = String.format("%04d", random.nextInt(RANDOM_BOUND));
        return "ORD-" + timestamp + "-" + suffix;
    }
}