package com.ecommerce.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Shared JWT parsing used at the edge to authenticate requests before they are
 * routed to a downstream service. Must be configured with the same signing
 * secret as user-service (where tokens are issued). Only parses/validates; it
 * never issues tokens.
 */
@Service
public class GatewayJwtService {

    private final SecretKey signingKey;

    public GatewayJwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Claim names mirror user-service's JwtService. */
    public static final String USER_ID_CLAIM = "uid";

    /**
     * Parses and verifies the token signature/expiry. Returns {@code null} when
     * the token is malformed, expired or has an invalid signature.
     */
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = toLong(claims.get(USER_ID_CLAIM));
            String email = claims.getSubject();
            List<?> rawAuthorities = claims.get("authorities", List.class);
            return new ParsedToken(userId, email, extractFirstRole(rawAuthorities));
        } catch (ExpiredJwtException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number num) {
            return num.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String extractFirstRole(List<?> authorities) {
        if (authorities == null) {
            return null;
        }
        return authorities.stream()
                .map(String::valueOf)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }

    public record ParsedToken(Long userId, String email, String role) {
    }
}
