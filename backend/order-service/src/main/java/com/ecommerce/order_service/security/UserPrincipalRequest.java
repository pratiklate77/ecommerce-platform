package com.ecommerce.order_service.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated caller for order-service, resolved from a validated JWT and made
 * available to controllers via {@code @AuthenticationPrincipal}. It carries the
 * numeric user id (from the token's {@code uid} claim) and the email (the
 * subject claim) so controllers never touch the raw {@code HttpServletRequest}
 * or parse headers themselves.
 *
 * <p>Mirrors the role user-service's {@code UserDetails} plays in its own
 * controllers, but is self-contained here (no dependency on user-service) and is
 * purpose-built for order requests.</p>
 */
public class UserPrincipalRequest implements UserDetails {

    private final Long userId;
    private final String email;
    private final List<GrantedAuthority> authorities;

    public UserPrincipalRequest(Long userId, String email) {
        this.userId = userId;
        this.email = email;
        this.authorities = List.of();
    }

    /** Numeric id from the token's {@code uid} claim. */
    public Long getUserId() {
        return userId;
    }

    /** Email from the token's subject claim. */
    public String getEmail() {
        return email;
    }

    // ---- UserDetails contract ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}