package ru.shortener.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
/**
 * Таблица links — хранилище сокращённых ссылок.
 *  Уникальное ограничение "uk_links_user_origin_url":
 *   - Предотвращает дубли: один пользователь → один URL → одна запись
 *
 *     Важно: если пользователь уже создал ссылку для URL,
 *    повторная попытка должна возвращать существующую ссылку.
 *    Важно: в columnNames указываются имена колонок в БД (origin_url, user_id),
 *    а не имена Java-полей (originalUrl). Это частая ошибка — если написать originalUrl,
 *    Hibernate создаст констрейнт по несуществующей колонке и упадёт на старте.
 */
@Table(
        name = "links",                                                // Имя таблицы в БД
        uniqueConstraints = @UniqueConstraint(                         // Составной уникальный ключ
                name = "uk_links_user_origin_url",                     // Имя ограничения (в БД)
                columnNames = {"user_id", "origin_url"}                // Колонки, участвующие в ограничении
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

    @Column(name = "create_at" )
    private LocalDateTime createdAt;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "user_id" )
    private Long userId;
}
