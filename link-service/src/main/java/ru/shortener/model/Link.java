package ru.shortener.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Таблица links — хранилище сокращённых ссылок.
 * <p>
 * Уникальное ограничение "uk_links_user_origin_url" предотвращает дубли:
 * один пользователь → один URL → одна запись.
 * <p>
 * Важно: в columnNames/indexes указываются имена колонок в БД (origin_url, user_id),
 * а не имена Java-полей (originalUrl). Это частая ошибка — Hibernate создаст
 * констрейнт по несуществующей колонке и упадёт на старте.
 */
@Entity
@Table(
        name = "links",
        uniqueConstraints = @UniqueConstraint(                 // Составной уникальный ключ
                name = "uk_links_user_origin_url",             // Имя ограничения (в БД)
                columnNames = {"user_id", "origin_url"}        // Колонки, участвующие в ограничении
        ),
        indexes = @Index(                                      // Ускоряет findByUserId (список ссылок пользователя)
                name = "idx_links_user_id",
                columnList = "user_id"
        )
)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "user_id" )
    private Long userId;
}
