package com.ecommerce.user_service.model;

/**
 * Roles a {@link User} can have. Authorit order of checks in the security layer
 * relies on these being stored/returned as {@code ROLE_<name>} authorities.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}