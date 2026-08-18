package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.shortener.exception.LinkNotFoundException;
import ru.shortener.model.Link;
import ru.shortener.repository.LinkRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {

    private final LinkRepository repository;

    private String generateShortCode() {
        String code = UUID.randomUUID().toString().substring(0,8);
        log.debug("Сгенерирован shortCode: {}", code);
        return code;
    }

    public Link createShortLink(String originalUrl) {
        Link link = new Link();
        link.setOriginalUrl(originalUrl);
        link.setShortCode(generateShortCode());
        link.setCreatedAt(LocalDateTime.now());
        Link saved = repository.save(link);
        log.debug("Ссылка сохранена с ID: {}, shortCode: {}", saved.getId(), saved.getShortCode());
        return saved;
    }

    public String getOriginalUrl(String shortCode) {
        log.debug("Поиск оригинального URL по shortCode: {}", shortCode);
        Link link = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {
                    log.warn("Ссылка не найдена: {}", shortCode);
                    return new LinkNotFoundException(shortCode);
                });
        link.setClickCount(link.getClickCount() + 1);
        log.debug("Обновление счетчика кликов по ссылке: {}", link);
        repository.save(link);
        return link.getOriginalUrl();
    }

    public Link getLink(String shortCode) {
        log.debug("Получение ссылки без увеличения счетчика кликов по shortCode: {}", shortCode);
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));
    }
}
