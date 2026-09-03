package ru.shortener.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.shortener.model.Link;
import ru.shortener.security.JwtAuthentication;
import ru.shortener.service.LinkService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Slf4j
public class LinkController {

    private final LinkService service;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthentication jwtAuth) {
            return jwtAuth.getUserId();
        }
        throw new IllegalStateException("Пользователь не авторизован");
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody LinkRequest request) {
        Long userId = getCurrentUserId();
        log.debug("Создание короткой ссылки для URL: {} от пользователя ID: {}",
                request.getOriginalUrl(), userId);
        Link link = service.createShortLink(request.getOriginalUrl(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(LinkResponse.from(link));
    }

    @GetMapping
    public ResponseEntity<List<LinkResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Long userId = getCurrentUserId();
        log.debug("GET: ссылки пользователя ID: {} (page: {}, size: {}, sort: {})",
                userId, page, size, (Object) sort);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sort));
        return ResponseEntity.ok(
                service.getUserLinks(userId, pageable).stream()
                        .map(LinkResponse::from)
                        .toList()
        );
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
    public ResponseEntity<LinkResponse> getStats(@PathVariable String shortCode) {
        Long userId = getCurrentUserId();
        log.debug("GET: статистика по ссылке: {} (userId: {})", shortCode, userId);
        Link link = service.getLink(shortCode, userId);
        return ResponseEntity.ok(LinkResponse.from(link));
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> delete(@PathVariable Long linkId) {
        Long userId = getCurrentUserId();
        log.debug("DELETE: удаление ссылки ID: {}, пользователем ID: {}", linkId, userId);
        service.deleteLink(linkId, userId);
        return ResponseEntity.noContent().build();
    }
}