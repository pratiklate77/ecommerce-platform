package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,

        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,

        @Size(max = 100, message = "lastName must be at most 100 characters")
        String lastName,

        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone
) {
}