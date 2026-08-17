package ru.shortener.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.shortener.model.Link;
import ru.shortener.service.LinkService;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService service;

    @PostMapping
    public ResponseEntity<Link> create (@RequestBody LinkRequest request) {
        Link link = service.createShortLink(request.getOriginUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.getOriginalUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", originalUrl)
                .build();
    }
}
