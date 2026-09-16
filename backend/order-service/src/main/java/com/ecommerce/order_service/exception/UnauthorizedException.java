package com.ecommerce.order_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated caller cannot be established (e.g. the API gateway
 * did not forward the caller identity) or when the action is not permitted.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}