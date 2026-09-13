package com.ecommerce.user_service.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}