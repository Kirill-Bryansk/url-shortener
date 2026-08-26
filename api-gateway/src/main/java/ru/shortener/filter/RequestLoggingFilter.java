package ru.shortener.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Глобальный фильтр шлюза: логирует каждый входящий запрос и ответ на него.
 *
 * Выходит ДО AuthFilter (order = -100), поэтому фиксирует в том числе
 * запросы, отклонённые с 401.
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    /** Чувствительные заголовки, значения которых не логируются */
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant start = Instant.now();
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod() != null ? request.getMethod().name() : "?";
        String path = request.getPath().value();
        String query = formatQuery(request);
        String remote = remoteAddress(request);
        String headers = safeHeaders(request);

        log.info("→ {} {}{} remote={} headers=[{}]", method, path, query, remote, headers);

        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpStatusCode status = response.getStatusCode();
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            log.info("← {} {}{} -> {} за {} мс", method, path, query,
                    status != null ? status.value() : 0, elapsed);
        });
    }

    /**
     * Собирает query-строку в виде ?k=v1&k=v2 (без чувствительных значений — вырезает пароли/токены).
     */
    private String formatQuery(ServerHttpRequest request) {
        if (request.getQueryParams().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        request.getQueryParams().forEach((key, values) -> {
            if (sb.length() > 1) {
                sb.append("&");
            }
            sb.append(key).append("=");
            if (SENSITIVE_HEADERS.contains(key.toLowerCase())) {
                sb.append("[FILTERED]");
            } else {
                sb.append(String.join(",", values));
            }
        });
        return sb.toString();
    }

    private String remoteAddress(ServerHttpRequest request) {
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "?";
    }

    /**
     * Перечисляет заголовки запроса, маскируя значения чувствительных
     * (Authorization, Cookie) как [FILTERED].
     */
    private String safeHeaders(ServerHttpRequest request) {
        StringBuilder sb = new StringBuilder();
        request.getHeaders().forEach((name, values) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name).append("=");
            if (SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                sb.append("[FILTERED]");
            } else {
                sb.append(String.join(",", values));
            }
        });
        return sb.toString();
    }

    @Override
    public int getOrder() {
        // Раньше, чем AuthFilter (у него порядок по умолчанию 0),
        // чтобы логировать и отклонённые запросы
        return -100;
    }
}
