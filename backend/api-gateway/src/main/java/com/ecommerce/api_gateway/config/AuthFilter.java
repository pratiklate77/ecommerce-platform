package com.ecommerce.api_gateway.config;

import com.ecommerce.api_gateway.security.GatewayJwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Gateway-wide authentication filter.
 *
 * <p>Authenticates the bearer JWT (issued by user-service) at the edge, strips
 * any client-supplied {@code X-User-*} headers to prevent spoofing, and forwards
 * the verified identity to the downstream service as {@code X-User-Id} /
 * {@code X-User-Email} / {@code X-User-Role}. Public routes (login/register and
 * product browsing) pass through unauthenticated.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_USER_ROLE = "X-User-Role";

    private final GatewayJwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (isPublicRequest(path, method)) {
            return chain.filter(withoutUserHeaders(exchange, null));
        }

        String authHeader = request.getHeaders().getFirst(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        GatewayJwtService.ParsedToken token = jwtService.parse(authHeader.substring(BEARER_PREFIX.length()));
        if (token == null || token.userId() == null) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        log.debug("Authenticated user {} (role={}) for {} {}", token.userId(), token.role(), method, path);
        return chain.filter(withoutUserHeaders(exchange, token));
    }

    /**
     * Returns a mutated exchange that has any spoofed client-supplied X-User-*
     * headers removed and (when a token is present) replaced with the verified
     * identity extracted from the token.
     */
    private ServerWebExchange withoutUserHeaders(ServerWebExchange exchange, GatewayJwtService.ParsedToken token) {
        ServerHttpRequest mutated = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(X_USER_ID);
            headers.remove(X_USER_EMAIL);
            headers.remove(X_USER_ROLE);
            if (token != null) {
                if (token.userId() != null) {
                    headers.set(X_USER_ID, String.valueOf(token.userId()));
                }
                headers.set(X_USER_EMAIL, token.email() == null ? "" : token.email());
                if (token.role() != null) {
                    headers.set(X_USER_ROLE, token.role());
                }
            }
        }).build();
        return exchange.mutate().request(mutated).build();
    }

    private boolean isPublicRequest(String path, HttpMethod method) {
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }
        // Product and inventory browsing (reads) are public; mutations are not.
        if ((path.startsWith("/api/v1/products") || path.startsWith("/api/v1/inventory"))
                && (method == HttpMethod.GET)) {
            return true;
        }
        return path.equals("/actuator/health") || path.equals("/health") || path.equals("/error");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\""
                + message + "\",\"fieldErrors\":null}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run before other gateway filters that may need the authenticated headers.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
