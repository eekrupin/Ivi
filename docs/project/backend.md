# Backend

## Состояние
Backend живет в `backend/` как отдельный Gradle root.

Стек:
- Kotlin `2.0.20`
- Ktor
- PostgreSQL
- Flyway
- Exposed DSL
- HikariCP
- BCrypt
- JWT access token
- opaque refresh token

Entrypoint:
- `backend/src/main/kotlin/ru/ekrupin/ivi/backend/Application.kt`
- запуск через `io.ktor.server.netty.EngineMain`

Конфигурация:
- `backend/src/main/resources/application.conf`
- env-шаблон: `backend/.env.example`

## Dev host/port
Dev backend должен слушать IPv4-интерфейс `0.0.0.0:8080`.

В `application.conf` default должен быть:

```hocon
ktor {
  deployment {
    host = "0.0.0.0"
    host = ${?HOST}

    port = 8080
    port = ${?PORT}
  }
}
```

В `.env.example`:

```properties
HOST=0.0.0.0
PORT=8080
```

Для проверки bind внутри WSL:

```bash
ss -lntp | grep 8080
```

Ожидается `*:8080` или `0.0.0.0:8080`.

## База данных
Backend подключается к PostgreSQL через JDBC/HikariCP.

Flyway применяет миграции при старте.

Схема уже включает:
- `users`
- `pets`
- `pet_memberships`
- `invites`
- `refresh_tokens`
- `event_types`
- `pet_events`
- `weight_entries`

Persistence строится через Exposed DSL, Exposed DAO сознательно не используется.

## Auth foundation
Модель входа: `email + password`.

Пароль хранится только как BCrypt hash в `users.password_hash`.

Access token:
- JWT Bearer token;
- короткоживущий;
- `userId` в `sub`.

Refresh token:
- opaque random token;
- на сервере хранится только SHA-256 hash;
- при refresh старый token ревокается и выдается новая пара.

Публичные endpoints:
- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `GET /health`

Требуют авторизации:
- `GET /v1/me`
- pet access;
- invites;
- sync.

## Pet access и invites
Модель доступа первого серверного этапа:
- один общий питомец;
- максимум два пользователя;
- роли `OWNER` и `MEMBER`;
- `OWNER` создает питомца и приглашает второго пользователя;
- `MEMBER` имеет полный доступ на чтение и изменение общих данных питомца.

Практическое ограничение V1: пользователь не может одновременно быть привязан к нескольким питомцам.

Invite flow:
- приглашение создает только `OWNER`;
- invite code имеет TTL от 1 до 168 часов, default 72 часа;
- принять может только авторизованный пользователь без другого активного membership;
- после принятия создается `MEMBER` membership, invite становится `ACCEPTED`.

Практические сценарии:
- тот же пользователь на втором устройстве логинится тем же аккаунтом и загружает данные уже существующего серверного питомца;
- новый пользователь без серверного питомца может отправить локальные данные на сервер, тогда будет создан отдельный питомец;
- второй человек для общего питомца принимает invite code от `OWNER`, после чего загружает общий server snapshot.

## Реализованные endpoints
- `GET /health`
- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `GET /v1/me`
- `POST /v1/pets`
- `GET /v1/pets/current`
- `POST /v1/pets/{petId}/invites`
- `POST /v1/invites/accept`
- `GET /v1/sync/bootstrap`
- `GET /v1/sync/changes`
- `POST /v1/sync/push`

Photo endpoints пока заведены как skeleton:
- `PUT /v1/pets/{petId}/photo`
- `DELETE /v1/pets/{petId}/photo`

## API-контракт
Source of truth — TypeSpec:
- `api/src/main.tsp`

OpenAPI — производный артефакт:
- `api/generated/openapi/openapi.yaml`

OpenAPI вручную не редактировать.

## Команды
Из корня репозитория:

```bash
./backend/gradlew -p ./backend build
./backend/gradlew -p ./backend run
docker compose -f ./backend/docker-compose.yml up -d
docker compose -f ./backend/docker-compose.yml down -v
```

Backend smoke/E2E:

```bash
node ./backend/e2e-smoke.mjs
```

Скрипт пишет отчет в `/tmp/ivi-e2e-report.json`.
