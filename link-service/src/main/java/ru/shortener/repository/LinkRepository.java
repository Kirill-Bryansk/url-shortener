package ru.shortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.shortener.model.Link;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

    @Modifying
    @Query("UPDATE Link l SET l.clickCount = l.clickCount + 1 WHERE l.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);

    Optional<Link> findByShortCode(String shortCode);
    Optional<Link> findByShortCodeAndUserId(String shortCode, Long userId);

    boolean existsByShortCode(String shortCode);

    boolean existsByOriginalUrlAndUserId(String originalUrl, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
    List<Link> findByUserId(Long userId);
    Optional<Link> findByIdAndUserId(Long id, Long userId);
}
