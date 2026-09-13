package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmailRequest(
        @NotBlank(message = "newEmail is required")
        @Email(message = "newEmail must be a valid address")
        @Size(max = 255, message = "newEmail must be at most 255 characters")
        String newEmail,

        @NotBlank(message = "password is required to confirm the change")
        String password
) {
}