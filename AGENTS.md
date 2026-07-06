# AGENTS.md

## Назначение
Короткий стартовый документ для агентской работы в проекте «Иви».

Подробный продуктовый и технический контекст вынесен в `docs/project/`, чтобы не загружать каждый сеанс лишними десятками тысяч символов. Если задача требует деталей, сначала открой соответствующий документ из раздела «Где искать контекст».

## Всегда важно
- Общаться с пользователем только на русском языке.
- Проектную документацию вести на русском языке.
- Все пользовательские тексты в приложении должны быть на русском языке.
- Перед существенными решениями сначала смотреть реальные файлы репозитория.
- Локальный Android MVP нельзя ломать ради архитектурной красоты.
- Android и backend развиваются раздельно; backend не встраивается в Android `app`-модуль.
- API source of truth — TypeSpec в `api/src/main.tsp`; generated OpenAPI вручную не редактировать.
- Реальные `local.properties`, `keystore.properties`, release keystore и секреты не должны попадать в git.
- Не откатывать и не перетирать чужие незакоммиченные изменения без явного запроса пользователя.

## Структура репозитория
- Android-клиент: `android/`, отдельный Gradle root, модуль `:app`.
- Backend: `backend/`, отдельный Gradle root на Kotlin/Ktor.
- API-контракт: `api/`, TypeSpec source и generated OpenAPI.
- Проектная документация: `docs/project/`.
- Будущий infra-слой: `infra/`.

Корневой package и `applicationId`: `ru.ekrupin.ivi`.

## Где искать контекст
- Продукт, предметная модель, бизнес-правила, UI-направление: `docs/project/product.md`.
- Android-архитектура, Room, уведомления, permissions, команды: `docs/project/android.md`.
- Backend, Ktor/PostgreSQL/Flyway/Exposed, auth, invites, host/port: `docs/project/backend.md`.
- Sync-модель, bootstrap/changes/push, outbox, conflict UX, WorkManager: `docs/project/sync.md`.
- Ручные проверки, Windows + WSL + emulator smoke, adb reverse, backend E2E: `docs/project/runbooks.md`.
- Журнал решений `D-001 ... D-036`: `docs/project/decisions.md`.
- Текущий статус, ближайшие шаги и отложенные задачи: `docs/project/roadmap.md`.

## Текущий фокус
- Stabilization / release candidate pass: не добавлять крупные новые фичи, проверять и фиксировать только реальные дефекты ownership, invite, leave/delete, owner transfer, photo sync и conflict flow.
- Критичные инварианты: у активного питомца всегда есть OWNER; MEMBER может выйти без удаления питомца для других; OWNER не может оставить питомца без владельца; transfer разрешён только активному участнику; delete серверного питомца разрешён только единственному OWNER после явного действия; stale invite к удалённому/недоступному питомцу не должен подключать пользователя; после leave/delete sync не должен возвращать старого питомца.
- Реализованные и проверяемые сценарии: синхронизация между устройствами, приглашение по коду, выход из синхронизации, отключение пользователя от питомца, удаление питомца единственным владельцем, передача прав владельца при выходе OWNER, синхронизация фото питомца.
- Последний stabilization pass добавил targeted backend integration tests для ownership/invite/photo и Android unit tests для no-server-pet/photo sync behavior; HTTP E2E подтвердил invite accept/repeat, owner transfer, member leave, запрет delete при active member, single-owner delete, stale invite и photo upload/download/delete.
- Ограничения после pass: device-level smoke через `wadb` частичный из-за хрупких координат Compose UI без test tags; Docker в текущей WSL-среде недоступен, поэтому backend PostgreSQL Testcontainers tests компилируются и skip локально, а полноценно выполняются при доступном Docker daemon.
- Следующий шаг перед первой beta: добавить стабильные Compose test tags или instrumentation сценарий для двухдевайсного E2E, затем повторить полный device smoke A-D на чистой backend DB.

## Основные команды
Android из корня репозитория:

```bash
./android/gradlew -p ./android :app:assembleDebug
./android/gradlew -p ./android test
./android/gradlew -p ./android :app:lintDebug
./android/gradlew -p ./android :app:assembleRelease
```

Backend из корня репозитория:

```bash
./backend/gradlew -p ./backend build
./backend/gradlew -p ./backend run
docker compose -f ./backend/docker-compose.yml up -d
docker compose -f ./backend/docker-compose.yml down -v
```

API-контракт внутри `api/`:

```bash
npm install
npm run generate:openapi
npm run build
```

## Локальный Android smoke
Короткая памятка:
- backend должен слушать `0.0.0.0:8080`;
- в WSL проверить `ss -lntp | grep 8080`, ожидается `*:8080` или `0.0.0.0:8080`;
- на Windows должен открываться `http://127.0.0.1:8080/health`;
- если Windows `localhost` работает, а `127.0.0.1` нет, см. portproxy в `docs/project/runbooks.md`;
- для emulator smoke через Windows adb выполнить `adb reverse tcp:8080 tcp:8080`;
- после `adb reverse` в приложении использовать backend URL `http://127.0.0.1:8080`;
- HTTP cleartext разрешен только в debug через `android/app/src/debug/AndroidManifest.xml`.

## Проверка после изменений
Типовой Android-порядок:

```bash
./android/gradlew -p ./android :app:assembleDebug
./android/gradlew -p ./android test
./android/gradlew -p ./android :app:lintDebug
```

Типовой backend-порядок:

```bash
./backend/gradlew -p ./backend build
```

Если задача точечная и пользователь явно указал конкретную проверку, выполнить указанную проверку и честно сообщить, что не запускалось.

## GitHub push
- Если пользователь явно просит `push`, перед пушем проверить `git remote get-url origin` и `gh auth status`.
- Если `origin` указывает на `https://github.com/...` и `gh auth status` показывает активную сессию, использовать одноразовый push через временный URL вида `https://x-access-token:$(gh auth token)@github.com/<owner>/<repo>.git`.
- Не менять `git remote` и `git config` ради push.

## ast-index
- Перед анализом кода проверить индекс командой `ast-index stats` из корня проекта.
- Если индекс не найден из подпапки, попробовать `ast-index --walk-up stats`.
- Если индекс отсутствует, создать `ast-index rebuild`.
- Если индекс устарел после изменений, выполнить `ast-index update`.
- Для структуры кода сначала использовать `ast-index explore`, `symbol`, `usages`, `callers`, `implementations`, `outline`.
- `grep`/`rg` использовать для точного текстового поиска, логов, строк, конфигов, JSON/YAML/XML/SQL/Markdown и случаев, когда `ast-index` не помог.

## Обновление документации
- Новые устойчивые продуктовые правила добавлять в `docs/project/product.md`.
- Android-детали добавлять в `docs/project/android.md`.
- Backend-детали добавлять в `docs/project/backend.md`.
- Sync-детали добавлять в `docs/project/sync.md`.
- Практические команды и ручные проверки добавлять в `docs/project/runbooks.md`.
- Архитектурные решения добавлять в `docs/project/decisions.md`.
- Текущий план и открытые вопросы добавлять в `docs/project/roadmap.md`.
- В `AGENTS.md` добавлять только то, что должно быть доступно агенту в каждом сеансе.
