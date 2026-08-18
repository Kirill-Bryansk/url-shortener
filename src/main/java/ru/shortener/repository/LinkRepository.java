package ru.shortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.shortener.model.Link;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);

    boolean existsByOriginalUrl(String originalUrl);
}
