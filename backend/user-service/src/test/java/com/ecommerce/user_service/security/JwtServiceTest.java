package com.ecommerce.user_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // At least 32 bytes for HS256.
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-signing!";
    private static final long EXPIRATION_MS = 3_600_000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS);

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("encoded")
                .authorities("ROLE_CUSTOMER")
                .build();
    }

    @Test
    void generatesTokenThatParsesBackToTheSubject() {
        String token = jwtService.generateToken(user("alice@example.com"));

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void tokenIsValidForItsOwnerButNotForSomeoneElse() {
        String token = jwtService.generateToken(user("alice@example.com"));

        assertThat(jwtService.isTokenValid(token, user("alice@example.com"))).isTrue();
        assertThat(jwtService.isTokenValid(token, user("bob@example.com"))).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService alreadyExpired = new JwtService(SECRET, -1_000L);
        String token = alreadyExpired.generateToken(user("alice@example.com"));

        assertThat(alreadyExpired.isTokenValid(token, user("alice@example.com"))).isFalse();
    }
}