package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "line1 is required")
        @Size(max = 255, message = "line1 must be at most 255 characters")
        String line1,

        @Size(max = 255, message = "line2 must be at most 255 characters")
        String line2,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @Size(max = 100, message = "state must be at most 100 characters")
        String state,

        @Size(max = 20, message = "postalCode must be at most 20 characters")
        String postalCode,

        @NotBlank(message = "country is required")
        @Size(max = 100, message = "country must be at most 100 characters")
        String country,

        boolean isDefault
) {
}