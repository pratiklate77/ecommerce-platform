package com.ecommerce.order_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request conflicts with the current state of a resource, e.g. an
 * invalid order status transition or an attempt to modify an order past a state
 * boundary.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}