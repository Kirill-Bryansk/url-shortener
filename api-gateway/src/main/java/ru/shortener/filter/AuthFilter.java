package ru.shortener.filter;

import ru.shortener.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtService jwtService;

    public AuthFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
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
                    return chain.filter(exchange);
                }
                log.warn("Нет Authorization header, запрос отклонён: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                // Один парсинг вместо двух
                var claims = jwtService.parseClaims(token);
                log.debug("Токен валидный: {} (ID: {})", claims.getSubject(), claims.get("userId", Long.class));
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