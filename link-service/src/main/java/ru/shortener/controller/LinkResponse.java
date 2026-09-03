package ru.shortener.controller;

import ru.shortener.model.Link;

import java.time.LocalDateTime;

/**
 * Публичный контракт API. Намеренно НЕ содержит userId:
 * клиент и так аутентифицирован, а внутренние данные наружу не отдаём.
 */
public record LinkResponse(
        Long id,
        String originalUrl,
        String shortCode,
        LocalDateTime createdAt,
        Long clickCount
) {
    public static LinkResponse from(Link link) {
        return new LinkResponse(
                link.getId(),
                link.getOriginalUrl(),
                link.getShortCode(),
                link.getCreatedAt(),
                link.getClickCount()
        );
    }
}
