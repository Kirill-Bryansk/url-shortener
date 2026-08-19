package ru.shortener.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.shortener.model.Link;
import ru.shortener.service.LinkService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Slf4j
public class LinkController {

    private final LinkService service;

    @PostMapping
    public ResponseEntity<Link> create(@Valid @RequestBody LinkRequest request,
                                       @RequestHeader("X-User-Id") Long userId) {
        log.debug("Создание короткой ссылки для URL: {} от пользователя ID: {}",
                request.getOriginalUrl(), userId);
        Link link = service.createShortLink(request.getOriginalUrl(), userId);
        log.debug("Короткая ссылка создана: {}", link.getShortCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @GetMapping
    public ResponseEntity<List<Link>> getAll(@RequestHeader("X-User-Id") Long userId) {
        log.debug("Получение всех ссылок пользователя: {}", userId);
        return ResponseEntity.ok(service.getUserLinks(userId));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        log.debug("Перенаправление по короткой ссылке: {}", shortCode);
        String originalUrl = service.getOriginalUrl(shortCode);
        log.debug("Перенаправление на оригинальный URL: {}", originalUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", originalUrl)
                .build();
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<Long> getStats(@PathVariable String shortCode) {
        log.debug("GET: статистика по ссылке: {}", shortCode);
        Link link = service.getLink(shortCode);
        return ResponseEntity.ok(link.getClickCount());
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> delete(@PathVariable Long linkId,
                                       @RequestHeader("X-User-Id") Long userId) {
        log.debug("DELETE: удаление ссылки ID: {}, пользователем ID: {}", linkId, userId);
        service.deleteLink(linkId, userId);
        return ResponseEntity.noContent().build();
    }
}