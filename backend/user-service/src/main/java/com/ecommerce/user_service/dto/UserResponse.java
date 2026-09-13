package com.ecommerce.user_service.dto;

import com.ecommerce.user_service.model.Role;
import com.ecommerce.user_service.model.User;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole()
        );
    }
}