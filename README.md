# URL Shortener

Микросервисный сервис сокращения ссылок на Spring Boot / Spring Cloud. Пользователь регистрируется, получает JWT, создаёт короткие ссылки, отслеживает статистику переходов.

## Архитектура

```
                     ┌──────────────┐
   Client ──8080──►  │ api-gateway  │  JWT-фильтр, маршрутизация
                     └──────┬───────┘
                            │  lb:// (Eureka)
                 ┌──────────┴───────────┐
                 ▼                      ▼
          ┌─────────────┐        ┌──────────────┐
          │ auth-service│        │ link-service │
          │  (users,    │        │   (links)    │
          │  refresh)   │        │              │
          └──────┬──────┘        └──────┬───────┘
                 └──────────┬───────────┘
                            ▼
                     ┌─────────────┐
                     │ PostgreSQL  │
                     └─────────────┘

  discovery-server (Eureka)   config-server (native configs)
```

## Сервисы

| Сервис | Порт | Назначение |
|---|---|---|
| `api-gateway` | 8080 | Единая точка входа, проверка JWT, маршрутизация |
| `auth-service` | 8081 | Регистрация, логин, refresh/logout, выдача токенов |
| `link-service` | 8082 | Создание ссылок, редирект, статистика |
| `config-server` | 8888 | Централизованная конфигурация (native, classpath) |
| `discovery-server` | 8761 | Service discovery (Eureka) |
| `postgres` | 5432 | БД `url_shortener` |
| `common-security` | — | Общая библиотека: `JwtService`, `JwtFilter`, `JwtAuthentication` |

## Стек

Java 21 · Spring Boot 3.4.5 · Spring Cloud 2024.0.1 · Eureka · Spring Cloud Gateway · Config Server · PostgreSQL 16 · JJWT 0.12.6 · Lombok · Maven (multi-module) · Docker Compose

## Быстрый старт

Нужны Docker и JDK 21.

```bash
# 1. Создать .env с секретом JWT (мин. 32 байта)
echo "JWT_SECRET=$(openssl rand -base64 48)" > .env

# 2. Собрать jar-ы (Dockerfile'ы копируют target/*.jar)
mvn clean package

# 3. Поднять всё
docker compose up --build -d

# 4. Проверить статус — все контейнеры должны быть (healthy)
docker compose ps
```

После старта API доступен на `http://localhost:8080`.

## Переменные окружения

| Переменная | Обязательна | Описание |
|---|---|---|
| `JWT_SECRET` | да | Секрет подписи JWT, минимум 32 байта. Без него сервисы не стартуют (fail-fast) |
| `EUREKA_HOST` | в Docker | Хост Eureka (`discovery-server` внутри compose) |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | в Docker | Подключение к PostgreSQL |

Секрет задаётся в `.env` (не коммитится в git). Для запуска из IDE — переменная окружения в Run Configuration.

## API

Все запросы идут через шлюз на `localhost:8080`. Защищённые эндпоинты требуют заголовок `Authorization: Bearer <access-token>`.

### Auth

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Регистрация, возвращает access + refresh |
| `POST` | `/api/v1/auth/login` | Логин, возвращает access + refresh |
| `POST` | `/api/v1/auth/refresh` | Обмен refresh-токена на новую пару |
| `POST` | `/api/v1/auth/logout` | Отзыв refresh-токена |

### Links

| Метод | Путь | Auth | Описание |
|---|---|---|---|
| `POST` | `/api/v1/links` | да | Создать короткую ссылку |
| `GET` | `/api/v1/links` | да | Список ссылок (пагинация, см. ниже) |
| `GET` | `/api/v1/links/{shortCode}` | нет | Редирект `302` на оригинальный URL |
| `GET` | `/api/v1/links/{shortCode}/stats` | да | Статистика по ссылке |
| `DELETE` | `/api/v1/links/{linkId}` | да | Удалить ссылку |

**Пагинация списка:** `GET /api/v1/links?page=0&size=20&sort=createdAt,desc`
- `size` ограничен 100; допустимые поля сортировки: `id`, `createdAt`, `clickCount`, `shortCode`.

### Пример

```bash
# Регистрация
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@mail.ru","password":"password123"}'

# Создание ссылки
curl -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com"}'

# Переход (302)
curl -i http://localhost:8080/api/v1/links/<shortCode>
```

## Аутентификация

- **Access-токен** — JWT, живёт 15 минут, проверяется шлюзом и сервисами.
- **Refresh-токен** — случайная строка, живёт 7 дней, хранится в БД в виде SHA-256 хеша.
- **Ротация:** каждый `/refresh` инвалидирует старый refresh-токен и выдаёт новый.
- **Детект кражи:** повторное использование отозванного токена отзывает все сессии пользователя.
- Истёкшие refresh-токены удаляются по расписанию (cron, 03:00).

## Тесты

```bash
mvn test
```

Юнит-тесты (JUnit 5 + Mockito + AssertJ) покрывают `JwtService`, `AuthService`, `RefreshTokenService`, `LinkService` — включая гонки, отзыв токенов и fail-fast валидацию секрета.

## Структура проекта

```
url-shortener/
├── api-gateway/        # шлюз, JWT-фильтр, логирование
├── auth-service/       # аутентификация, refresh-токены
├── link-service/       # ссылки, редирект, статистика
├── common-security/    # общая JWT-библиотека
├── config-server/      # конфигурация сервисов (configs/*.yml)
├── discovery-server/   # Eureka
├── docker-compose.yml
└── pom.xml             # parent POM
```
