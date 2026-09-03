package ru.shortener.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Общий JWT-фильтр. НЕ помечен @Component: бин создаёт каждый сервис
 * в своём SecurityConfig. Это сознательно — component scan не должен
 * подхватывать servlet-фильтры в reactive-приложениях (api-gateway).
 */
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            Long userId = jwtService.extractUserId(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                JwtAuthentication auth = new JwtAuthentication(email, userId);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Аутентифицирован пользователь: {} (ID: {})", email, userId);
            }
        } catch (Exception e) {
            log.warn("Невалидный токен: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}