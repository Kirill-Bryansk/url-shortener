package ru.shortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.shortener.model.Link;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    boolean existsByOriginalUrlAndUserId(String originalUrl, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
    List<Link> findByUserId(Long userId);
    Optional<Link> findByIdAndUserId(Long id, Long userId);
}
