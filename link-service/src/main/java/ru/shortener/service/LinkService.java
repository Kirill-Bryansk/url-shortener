package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shortener.exception.DuplicateException;
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
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8);
        } while (repository.existsByShortCode(code)); // защита от коллизий
        return code;
    }

    @Transactional
    public Link createShortLink(String originalUrl, Long userId) {
        checkLink(originalUrl, userId);
        Link link = new Link();
        link.setOriginalUrl(originalUrl);
        link.setShortCode(generateShortCode());
        link.setCreatedAt(LocalDateTime.now());
        link.setUserId(userId);

        try {
            Link saved = repository.saveAndFlush(link);
            log.debug("Ссылка сохранена с ID: {}, shortCode: {}", saved.getId(), saved.getShortCode());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateException("Такая ссылка уже существует");
        }
    }

    @Transactional
    public String getOriginalUrl(String shortCode) {
        log.debug("Поиск оригинального URL по shortCode: {}", shortCode);
        Link link = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {
                    log.warn("Ссылка не найдена: {}", shortCode);
                    return new LinkNotFoundException(shortCode);
                });
        repository.incrementClickCount(shortCode);
        return link.getOriginalUrl();
    }

    public Link getLink(String shortCode, Long userId) {
        log.debug("Получение ссылки без увеличения счетчика кликов по shortCode: {} (userId: {})", shortCode, userId);
        return repository.findByShortCodeAndUserId(shortCode, userId)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));
    }

    public Page<Link> getUserLinks(Long userId, Pageable pageable) {
        log.debug("Получение списка ссылок пользователя с ID: {} (page: {})", userId, pageable);
        return repository.findByUserId(userId, pageable);
    }

    @Transactional
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