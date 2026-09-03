package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shortener.exception.InvalidCredentialsException;
import ru.shortener.model.RefreshToken;
import ru.shortener.model.User;
import ru.shortener.repository.RefreshTokenRepository;
import ru.shortener.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /** Выдаёт новый refresh-токен: клиенту — сырую строку, в БД — только хеш. */
    @Transactional
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(sha256(rawToken));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    /**
     * Ротация: проверяет токен, отзывает его и возвращает пользователя
     * для выпуска новой пары. Повторное использование отозванного токена
     * трактуется как кража — отзываются ВСЕ сессии пользователя.
     */
    @Transactional
    public User rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException("Невалидный refresh-токен"));

        if (stored.isRevoked()) {
            log.warn("Обнаружено повторное использование refresh-токена, отзыв всех сессий пользователя ID: {}",
                    stored.getUserId());
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw new InvalidCredentialsException("Сессия отозвана, войдите заново");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh-токен истёк");
        }

        stored.setRevoked(true); // ротация: старый токен больше не действует
        return userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    /** Разовая проверка хеширования: SHA-256 есть в любой JVM, исключение невозможно. */
    // package-private (без private), чтобы юнит-тест вычислял хеш так же, как сервис
    String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")  // каждый день в 03:00
    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}