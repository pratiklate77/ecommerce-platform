package com.ecommerce.order_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Stateless JWT verifier for order-service. It shares the user-service signing
 * secret ({@code app.jwt.secret}) so it can trust tokens issued by user-service
 * without a database lookup or a call to the API gateway. This lets clients hit
 * the service directly on its own port.
 */
@Service
public class JwtService {

    /** Claim key holding the user's numeric id (mirrors user-service and the gateway). */
    public static final String USER_ID_CLAIM = "uid";

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the user's numeric id from the {@code uid} claim, or {@code null}
     * when the token is missing that claim, is malformed, expired or has an
     * invalid signature.
     */
    public Long extractUserId(String token) {
        try {
            Integer raw = parse(token).get(USER_ID_CLAIM, Integer.class);
            return raw == null ? null : raw.longValue();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /** Returns the subject (email) claim, or {@code null} on an invalid token. */
    public String extractUsername(String token) {
        try {
            return parse(token).getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /** True when the token has a valid signature and is not expired. */
    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
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