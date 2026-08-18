package ru.shortener.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.shortener.model.Link;
import ru.shortener.service.LinkService;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Slf4j
public class LinkController {

    private final LinkService service;

    @PostMapping
    public ResponseEntity<Link> create (@Valid @RequestBody LinkRequest request) {
        log.debug("Создание короткой ссылки для URL: {}", request.getOriginalUrl());
        Link link = service.createShortLink(request.getOriginalUrl());
        log.debug("Короткая ссылка создана: {}", link.getShortCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
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
        log.debug("GET: получения статистики по короткой ссылке: {}", shortCode);
        Link link = service.getLink(shortCode);
        return ResponseEntity.ok(link.getClickCount());
    }
}
