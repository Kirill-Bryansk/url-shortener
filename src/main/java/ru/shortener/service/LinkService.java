package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.shortener.exception.LinkNotFoundException;
import ru.shortener.model.Link;
import ru.shortener.repository.LinkRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository repository;

    private String generateShortCode() {
        return UUID.randomUUID().toString().substring(0,8);
    }

    public Link createShortLink(String originalUrl) {
        Link link = new Link();
        link.setOriginalUrl(originalUrl);
        link.setShortCode(generateShortCode());
        link.setCreatedAt(LocalDateTime.now());
        return repository.save(link);
    }

    public String getOriginalUrl(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode))
                .getOriginalUrl();
    }
}
