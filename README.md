# Иви

«Иви» — семейное приложение для учета событий по одному питомцу, собаке по имени Иви.

Проект состоит из Android-клиента, отдельного backend-приложения и API-контракта. Android остается local-first: UI работает от локальной Room-базы, а backend используется для аккаунтов, общего доступа, синхронизации между устройствами и синхронизации фото питомца.

## Что это

«Иви» помогает вести понятную историю ухода за собакой без лишней сложности:

- карточка питомца с фото, именем и датой рождения;
- журнал событий с типами, статусами и датами контроля;
- история веса;
- локальные напоминания о важных датах;
- синхронизация общих данных между устройствами;
- приглашение второго пользователя к общему питомцу;
- спокойный Compose UI на русском языке.

## Структура репозитория

- `android/` — Android-клиент, отдельный Gradle root, модуль `:app`.
- `backend/` — Kotlin/Ktor backend, отдельный Gradle root.
- `api/` — TypeSpec API-контракт и generated OpenAPI.
- `docs/project/` — подробная проектная документация.
- `infra/` — зарезервировано под будущий production/infra-слой.

API source of truth — `api/src/main.tsp`. Сгенерированный OpenAPI в `api/generated/openapi/openapi.yaml` вручную не редактируется.

## Что уже реализовано

Android:

- один текущий питомец в рамках MVP;
- локальное хранение на `Room`;
- CRUD для питомца, типов событий, событий, веса и настроек напоминаний;
- фильтрация событий по статусам `ACTIVE`, `COMPLETED`, `ARCHIVED`;
- удаление события из режима редактирования;
- удаление типа события с правилом: без связанных событий удаляется, при наличии связанных событий деактивируется;
- выбор фото питомца через системный Android Photo Picker;
- локальное копирование фото во внутреннее хранилище приложения;
- синхронизация фото через отдельный backend binary API;
- локальные уведомления через `AlarmManager`, `BroadcastReceiver` и `NotificationChannel`;
- перепланирование напоминаний после изменений и после перезагрузки устройства;
- экран подключения к синхронизации: backend URL, email, пароль, login/register;
- foreground, manual и background sync foundation;
- Conflict UX V1 с действиями `Принять серверную версию` и `Повторить мои изменения`;
- release-сборка без debug seed-данных.

Backend:

- PostgreSQL-backed auth: register, login, refresh;
- хранение паролей как BCrypt hash;
- JWT access token и opaque refresh token с хранением SHA-256 hash на сервере;
- pet access и memberships с ролями `OWNER` и `MEMBER`;
- invite flow для подключения второго пользователя;
- sync endpoints `bootstrap`, `changes`, `push`;
- optimistic concurrency и conflict response для sync push;
- photo endpoints для upload/download/delete актуального фото питомца;
- Flyway-миграции при старте;
- health endpoint `GET /health`;
- backend smoke/E2E script.

## Технологии

Android:

- Kotlin `2.0.20`
- Android Gradle Plugin `8.5.2`
- Gradle `8.7`
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- Hilt
- Coroutines + Flow
- Coil
- DataStore
- WorkManager
- AlarmManager
- `compileSdk 35`
- `minSdk 26`
- `targetSdk 35`
- Java `17`

Backend:

- Kotlin `2.0.20`
- Ktor
- PostgreSQL
- Flyway
- Exposed DSL
- HikariCP
- BCrypt
- JWT
- Java `17`

API:

- TypeSpec
- OpenAPI 3.1 generated artifact

## Требования к окружению

- JDK `17`.
- Android Studio с поддержкой AGP `8.5.x`.
- Android SDK `35`.
- Docker для локального PostgreSQL backend-разработки.
- Node.js/npm для работы с `api/` и запуска backend smoke/E2E.

Для сборки Android из корня репозитория через wrapper используется `./android/gradlew -p ./android`.

Для сборки backend из корня репозитория через wrapper используется `./backend/gradlew -p ./backend`.

## Быстрый старт

Собрать Android debug APK:

```bash
./android/gradlew -p ./android :app:assembleDebug
```

Debug-сборка подписывается стандартным debug-ключом Android. При первом запуске debug-версии пустая база автоматически заполняется демо-данными для локальной разработки и проверки UX.

Release-сборка стартует без демо-данных.

Поднять локальный PostgreSQL для backend:

```bash
docker compose -f ./backend/docker-compose.yml up -d
```

Запустить backend локально:

```bash
./backend/gradlew -p ./backend run
```

Проверить backend:

```bash
curl http://127.0.0.1:8080/health
```

## Полезные команды

Android debug-сборка:

```bash
./android/gradlew -p ./android :app:assembleDebug
```

Android unit-тесты:

```bash
./android/gradlew -p ./android test
```

Android lint для debug:

```bash
./android/gradlew -p ./android :app:lintDebug
```

Android release-сборка:

```bash
./android/gradlew -p ./android :app:assembleRelease
```

Backend build:

```bash
./backend/gradlew -p ./backend build
```

Backend run:

```bash
./backend/gradlew -p ./backend run
```

Локальный PostgreSQL для backend:

```bash
docker compose -f ./backend/docker-compose.yml up -d
```

Остановить локальный PostgreSQL с удалением volume:

```bash
docker compose -f ./backend/docker-compose.yml down -v
```

Backend smoke/E2E:

```bash
node ./backend/e2e-smoke.mjs
```

Smoke против опубликованного backend:

```bash
IVI_BASE_URL=https://ivi.eekrupin.ru node ./backend/e2e-smoke.mjs
```

API contract:

```bash
cd api
npm install
npm run generate:openapi
npm run build
```

Рекомендуемый порядок проверки после Android-изменений:

```bash
./android/gradlew -p ./android :app:assembleDebug
./android/gradlew -p ./android test
./android/gradlew -p ./android :app:lintDebug
```

Рекомендуемая проверка после backend-изменений:

```bash
./backend/gradlew -p ./backend build
```

## Локальный Android smoke против backend

Для локального smoke backend должен слушать IPv4-интерфейс `0.0.0.0:8080`.

В WSL проверить bind можно так:

```bash
ss -lntp | grep 8080
```

Ожидается `*:8080` или `0.0.0.0:8080`.

Если Windows открывает `http://localhost:8080/health`, но не открывает `http://127.0.0.1:8080/health`, настройте portproxy в PowerShell от администратора:

```powershell
$wslIp = (wsl hostname -I).Trim().Split()[0]

netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=8080

netsh interface portproxy add v4tov4 `
  listenaddress=127.0.0.1 `
  listenport=8080 `
  connectaddress=$wslIp `
  connectport=8080

netsh interface portproxy show all
```

После этого на Windows должен открываться:

```text
http://127.0.0.1:8080/health
```

Для Android-эмулятора выполнить `adb reverse` через Windows Android SDK:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 reverse tcp:8080 tcp:8080
```

`emulator-5554` замените на имя устройства из `adb devices`. После `adb reverse` в Android-эмуляторе используйте backend URL:

```text
http://127.0.0.1:8080
```

HTTP cleartext разрешен только в debug-сборке через `android/app/src/debug/AndroidManifest.xml`. Release-сборка должна использовать HTTPS backend URL, например:

```text
https://ivi.eekrupin.ru
```

## Release-подпись

В проекте подготовлена настройка release signing через файл `android/keystore.properties` в Android Gradle root.

В git должен храниться только шаблон `android/keystore.properties.example`.

Реальные файлы не должны коммититься и остаются только локально:

- `android/keystore.properties`
- сам release keystore (`.keystore` или `.jks`)

Минимальные поля:

```properties
storeFile=/absolute/path/to/your-release.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Если `keyPassword` не указан, будет использован `storePassword`.

`storeFile` можно задавать абсолютным путем или путем относительно Android root `android/`. Например, локальный файл рядом с `keystore.properties` можно указать как `storeFile=ivi-release.keystore`.

При запуске release-задач без настроенной подписи Gradle завершит сборку с ошибкой.

Пример release-сборки:

```bash
./android/gradlew -p ./android :app:assembleRelease
```

Даже корректно подписанный APK при ручной установке может восприниматься Android как установка из неизвестного источника. Для обычной пользовательской установки нужен релиз через Google Play, например через `Internal testing`.

## Backend-конфигурация

Backend читает настройки из env. Шаблон находится в `backend/.env.example`.

Основные переменные:

```properties
PORT=8080
HOST=0.0.0.0
DB_JDBC_URL=jdbc:postgresql://localhost:55432/ivi
DB_USERNAME=ivi
DB_PASSWORD=ivi
AUTH_JWT_SECRET=change-me-local-dev-secret
AUTH_JWT_ISSUER=ivi-backend
AUTH_JWT_AUDIENCE=ivi-clients
AUTH_ACCESS_TTL_SECONDS=3600
AUTH_REFRESH_TTL_SECONDS=2592000
```

Dev-значения подходят только для локальной разработки. Для production нужны отдельные сильные `DB_PASSWORD` и `AUTH_JWT_SECRET`, а PostgreSQL не должен быть открыт наружу в интернет.

## Реализованные backend endpoints

Публичные:

- `GET /health`
- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`

Требуют авторизации:

- `GET /v1/me`
- `POST /v1/pets`
- `GET /v1/pets/current`
- `POST /v1/pets/{petId}/invites`
- `POST /v1/invites/accept`
- `GET /v1/sync/bootstrap`
- `GET /v1/sync/changes`
- `POST /v1/sync/push`
- `GET /v1/pets/{petId}/photo`
- `PUT /v1/pets/{petId}/photo`
- `DELETE /v1/pets/{petId}/photo`

## Архитектура

Android-часть живет в `android/` как отдельный Gradle root и состоит из одного модуля `:app`:

- `app/` — entrypoint, DI, навигация, тема;
- `core/` — общие UI-компоненты и утилиты;
- `data/` — Room, DAO, entity, repository-реализации, sync, сеть, DataStore и напоминания;
- `domain/` — модели и repository-контракты;
- `feature/` — экраны и `ViewModel` отдельных сценариев.

Backend-часть живет в `backend/` как отдельный Gradle root. Persistence строится через Exposed DSL, миграции применяются Flyway при старте, основная БД — PostgreSQL.

Android не зависит напрямую от backend-реализации. API-контракт и sync-модель живут в `api/` отдельно от клиентского и серверного кода.

## Данные, sync и напоминания

- Android UI работает от Room, а не напрямую от сетевых DTO.
- После подключения аккаунта сервер становится источником истины для общих данных питомца.
- Room остается локальным кэшем и offline-базой.
- Sync V1 строится вокруг `bootstrap`, `changes` и `push`.
- Фото питомца синхронизируется отдельным binary API, а JSON sync передает `photoRevision`.
- Reminder settings в первом sync-этапе не синхронизируются.
- Напоминания локальные и планируются только для событий со статусом `ACTIVE` и заполненной датой контроля.
- В debug при пустой базе создаются стартовые данные.
- В release автозаполнение отключено.

## Ограничения текущего этапа

Сейчас в проект не входят:

- web-версия;
- несколько питомцев;
- server-side push-уведомления;
- сложная ролевая модель;
- вложения и документы;
- production infra как оформленный слой репозитория.

Практические ограничения V1:

- у пользователя один текущий серверный питомец;
- первый сценарий общего доступа ориентирован на семейное использование и небольшое число участников;
- `MEMBER` может выйти из серверного питомца;
- `OWNER` не может оставить питомца без владельца: нужен transfer ownership или удаление серверного питомца, если владелец единственный активный участник.

## Документация

Стартовый документ для агентской работы — `AGENTS.md`.

Подробный проектный контекст разнесен по `docs/project/`:

- `docs/project/product.md`
- `docs/project/android.md`
- `docs/project/backend.md`
- `docs/project/sync.md`
- `docs/project/runbooks.md`
- `docs/project/decisions.md`
- `docs/project/roadmap.md`

В этих документах зафиксированы продуктовые границы, архитектурные договоренности, бизнес-правила, текущее состояние реализации и рабочие команды.
