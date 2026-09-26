package com.ecommerce.product_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stateless JWT verifier for product-service. It shares the user-service signing
 * secret ({@code app.jwt.secret}) so it can trust tokens issued by user-service
 * without needing a database lookup or any call to another service.
 */
@Service
public class JwtService {

    private static final String AUTHORITIES_CLAIM = "authorities";

    /** Claim key holding the user's numeric id (mirrors user-service). */
    public static final String USER_ID_CLAIM = "uid";

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns true when the token has a valid signature and is not expired. */
    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** Extracts the subject (email) claim. Throws on an invalid token. */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /**
     * Extracts the granted authorities (e.g. {@code ROLE_ADMIN} / {@code ROLE_CUSTOMER})
     * embedded by user-service. Returns an empty set when the claim is missing.
     */
    public Set<String> extractAuthorities(String token) {
        Object raw = parse(token).get(AUTHORITIES_CLAIM);
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /** Extracts the user id claim, or {@code null} when absent. */
    public Long extractUserId(String token) {
        try {
            Integer raw = parse(token).get(USER_ID_CLAIM, Integer.class);
            return raw == null ? null : raw.longValue();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
