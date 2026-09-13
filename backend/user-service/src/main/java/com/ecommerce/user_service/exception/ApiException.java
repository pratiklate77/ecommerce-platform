package com.ecommerce.user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected, business-rule exceptions. Subclasses carry an HTTP
 * status so the {@code GlobalExceptionHandler} can translate them consistently.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}