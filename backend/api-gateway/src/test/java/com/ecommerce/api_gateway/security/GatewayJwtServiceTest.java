package com.ecommerce.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

class GatewayJwtServiceTest {

    private static final String SECRET = "change-me-to-a-32-byte-min-secret-key-for-hs256!!-dev-only";

    private GatewayJwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new GatewayJwtService(SECRET);
    }

    private String token(Long userId, String email, List<String> authorities, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("authorities", authorities)
                .claim(GatewayJwtService.USER_ID_CLAIM, userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    @Test
    void parsesValidTokenWithIdentity() {
        String t = token(42L, "admin@example.com", List.of("ROLE_ADMIN"), 60000);

        GatewayJwtService.ParsedToken parsed = jwtService.parse(t);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals(42L, parsed.userId());
        Assertions.assertEquals("admin@example.com", parsed.email());
        Assertions.assertEquals("ADMIN", parsed.role());
    }

    @Test
    void extractsCustomerRole() {
        String t = token(7L, "c@example.com", List.of("ROLE_CUSTOMER"), 60000);
        Assertions.assertEquals("CUSTOMER", jwtService.parse(t).role());
    }

    @Test
    void nullForExpiredToken() {
        String t = token(1L, "x@example.com", List.of("ROLE_CUSTOMER"), -1000);
        Assertions.assertNull(jwtService.parse(t));
    }

    @Test
    void nullForGarbageToken() {
        Assertions.assertNull(jwtService.parse("not-a-jwt"));
    }
}
