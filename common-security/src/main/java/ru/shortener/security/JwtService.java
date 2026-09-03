package ru.shortener.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String ISSUER = "url-shortener";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key;

    /**
     * Fail-fast: без валидного секрета сервис не должен стартовать.
     * HS256 требует ключ длиной не менее 32 байт (256 бит).
     */
    @PostConstruct
    void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "jwt.secret: не задан или короче 32 байт (требование HS256)");
        }
        if (secret.startsWith("your-very-long-secret-key")) {
            throw new IllegalStateException(
                    "jwt.secret: используется дефолтный секрет из репозитория — задай JWT_SECRET");
        }
        // Ключ вычисляется один раз за жизнь приложения
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email) {
        return Jwts.builder()
                .issuer(ISSUER)                       // ← iss, проверяется при парсинге
                .subject(email)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Единственная точка парсинга: верификация подписи и issuer происходит один раз.
     * Невалидный/просроченный/чужой токен → JwtException ещё здесь.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }
}