package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional fields the authenticated user may change on their own profile.
 * Null / omitted fields are left untouched by the service.
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,

        @Size(max = 100, message = "lastName must be at most 100 characters")
        String lastName,

        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone
) {
}