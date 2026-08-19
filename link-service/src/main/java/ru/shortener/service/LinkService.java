package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.shortener.exception.DuplicateException;
import ru.shortener.exception.LinkNotFoundException;
import ru.shortener.model.Link;
import ru.shortener.repository.LinkRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {

    private final LinkRepository repository;

    private String generateShortCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8);
        } while (repository.existsByShortCode(code)); // защита от коллизий
        log.debug("Сгенерирован shortCode: {}", code);
        return code;
    }

    public Link createShortLink(String originalUrl, Long userId) {
        checkLink(originalUrl, userId);
        Link link = new Link();
        link.setOriginalUrl(originalUrl);
        link.setShortCode(generateShortCode());
        link.setCreatedAt(LocalDateTime.now());
        link.setUserId(userId);
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
        // Защита от NPE, если в БД у старой записи click_count = NULL
        long currentClicks = link.getClickCount() == null ? 0L : link.getClickCount();
        link.setClickCount(currentClicks + 1);
        log.debug("Обновление счетчика кликов по ссылке: {}", link);
        repository.save(link);
        return link.getOriginalUrl();
    }

    public Link getLink(String shortCode) {
        log.debug("Получение ссылки без увеличения счетчика кликов по shortCode: {}", shortCode);
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));
    }

    public List<Link> getUserLinks(Long userId) {
        log.debug("Получение списка ссылок пользователя с ID: {}", userId);
        return repository.findByUserId(userId);
    }

    public void deleteLink(Long linkId, Long userId) {
        log.debug("Удаление ссылки ID: {} пользователем ID: {}", linkId, userId);

        repository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не найдена или не принадлежит вам"));

        repository.deleteByIdAndUserId(linkId, userId);
        log.debug("Ссылка удалена: {}", linkId);
    }

    public void checkLink(String originalUrl, Long userId) {
        if (repository.existsByOriginalUrlAndUserId(originalUrl, userId)) {
            throw new DuplicateException("Такая ссылка уже существует");
        }
    }
}