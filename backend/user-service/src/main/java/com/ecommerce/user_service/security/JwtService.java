package com.ecommerce.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Claim key holding the user's numeric id. Downstream services (and the API
     * Gateway, which forwards it as the {@code X-User-Id} header) need this to
     * scope resources without an extra lookup.
     */
    public static final String USER_ID_CLAIM = "uid";

    /**
     * Builds a signed JWT for the given user, embedding the subject (username/email)
     * and the granted authorities as a claim.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    /**
     * Builds a signed JWT for the given user, also embedding the user's numeric id
     * under {@link #USER_ID_CLAIM} so downstream can resolve resource ownership.
     */
    public String generateToken(UserDetails userDetails, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var jwtsBuilder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("authorities", authorities);
        if (userId != null) {
            jwtsBuilder.claim(USER_ID_CLAIM, userId);
        }

        return jwtsBuilder
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates that the token is well-formed, not expired and belongs to the given user.
     * Returns {@code false} for any flawed/expired token instead of throwing.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractClaim(token, Claims::getSubject);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user id claim, or {@code null} when the token predates the
     * {@code uid} claim (e.g. legacy or externally-issued tokens).
     */
    public Long extractUserId(String token) {
        try {
            Integer raw = extractClaim(token, claims -> claims.get(USER_ID_CLAIM, Integer.class));
            return raw == null ? null : raw.longValue();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}