package ru.shortener.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            // Публичный редирект по короткой ссылке: GET /api/v1/links/{shortCode} доступен без токена
            boolean isPublicRedirect = exchange.getRequest().getMethod() == HttpMethod.GET
                    && isRedirectPath(path);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                if (isPublicRedirect) {
                    log.debug("Публичный редирект без токена: {}", path);
                    // Не передаём клиентский X-User-Id во внутренний сервис
                    exchange = exchange.mutate()
                            .request(r -> r.headers(headers -> headers.remove("X-User-Id")))
                            .build();
                    return chain.filter(exchange);
                }
                log.warn("Нет Authorization header, запрос отклонён: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String email = claims.getSubject();
                Long userId = claims.get("userId", Long.class);

                log.debug("Токен валидный: {} (ID: {})", email, userId);

                // Всегда перезаписываем X-User-Id значением из токена,
                // чтобы клиент не мог подставить чужой userId
                exchange = exchange.mutate()
                        .request(r -> r.headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.set("X-User-Id", String.valueOf(userId));
                        }))
                        .build();

            } catch (Exception e) {
                log.warn("Невалидный токен: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Проверяет, что путь имеет вид /api/v1/links/{shortCode} — ровно один сегмент после префикса.
     * Примеры:
     * /api/v1/links/abc123    -> true  (редирект)
     * /api/v1/links/abc/stats -> false (статистика — требует авторизации)
     * /api/v1/links           -> false (список — требует авторизации)
     */
    private boolean isRedirectPath(String path) {
        String prefix = "/api/v1/links";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String rest = path.substring(prefix.length());
        return rest.matches("/[^/]+");
    }

    public static class Config {
        // пустой
    }
}