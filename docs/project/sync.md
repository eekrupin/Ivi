# Синхронизация

## Общий подход
Android остается local-first клиентом: UI работает от Room, а не напрямую от сетевых DTO.

После появления аккаунтов и sync сервер становится источником истины для общих данных питомца, но Android сохраняет Room как локальный кэш и рабочую offline-базу.

API-контракт и sync-модель живут отдельно от клиентского и серверного кода.

## Remote-модель V1
Сервер хранит только общие данные, которые разделяются между пользователями:
- `User`
- `Pet`
- `PetMembership`
- `EventType`
- `PetEvent`
- `WeightEntry`

Правила:
- для syncable-сущностей использовать стабильные UUID;
- у каждой syncable-записи есть `updatedAt`, `version`, `deletedAt`;
- `ReminderSettings` не синхронизируются в первом sync-этапе;
- фото питомца должно идти отдельным binary API, не через JSON sync-поток;
- текущий Android `photoUri` остается локальным указателем на кэш или локальную копию.

## API sync endpoints
- `GET /v1/sync/bootstrap`
- `GET /v1/sync/changes?cursor=...`
- `POST /v1/sync/push`

Главный мобильный контракт строится вокруг `bootstrap`, `changes`, `push`, а не вокруг отдельных CRUD endpoints на каждый экран.

## Bootstrap
`bootstrap` возвращает полный snapshot доступного питомца:
- текущий пользователь;
- текущий питомец;
- активные membership текущего питомца;
- `event_types`;
- `pet_events`;
- `weight_entries`;
- cursor вида `bootstrap:<epochMillis>`.

Для sync-consistency bootstrap включает soft-deleted записи с заполненным `deletedAt`, чтобы клиент не resurrect-ил удаленные объекты после потери cursor.

Android bootstrap import в V1:
- разрешен только при пустом `sync_outbox`;
- выполняет authoritative replace server-backed данных;
- локальный `photoUri` питомца сохраняется.

## Changes
`changes` работает по модели `(cursor, highWatermark]`:
- сервер декодирует cursor во внутренний timestamp;
- выбирает новый `highWatermark = now()`;
- возвращает записи с `updatedAt > cursorTime` и `updatedAt <= highWatermark`;
- обычные изменения идут в `changes.*`;
- soft-deleted записи идут в `tombstones`;
- ответ выдает cursor вида `changes:<epochMillis>`.

Сервер принимает cursor форматов:
- `bootstrap:<epochMillis>`
- `changes:<epochMillis>`

Невалидный cursor приводит к `409 invalid_sync_cursor`, отсутствие cursor — к `400 missing_sync_cursor`. Клиент может fallback-нуть на bootstrap.

## Push
`sync/push` V1 поддерживает:
- `EVENT_TYPE`
- `PET_EVENT`
- `WEIGHT_ENTRY`

Пакет применяется по фиксированному порядку зависимостей.

Optimistic concurrency:
- сервер принимает изменение только если `baseVersion` клиента совпадает с текущей серверной версией;
- при несовпадении возвращает conflict с актуальной server record;
- для V1 серверная версия считается победившей.

Android drain outbox:
- берет `PENDING` записи из `sync_outbox`;
- помечает их `IN_FLIGHT`;
- отправляет пакет mutations;
- accepted-записи удаляет из outbox;
- для accepted обновляет `serverVersion`, `serverUpdatedAt`, `syncState = SYNCED`, `lastSyncedAt`;
- conflicts переводит в `FAILED`, локальную сущность — в `CONFLICT`;
- `requiresBootstrap = true` фиксирует в `sync_state`.

## App-level orchestration
`RunFullSyncUseCase` объединяет:
- bootstrap import;
- changes pull;
- drain outbox.

Порядок V1:
- если `requiresBootstrap = true` или cursor отсутствует, при пустом outbox выполняется bootstrap;
- если cursor есть, при наличии pending outbox сначала push, затем changes;
- если bootstrap нужен, но outbox не пустой, возвращается `RequiresBootstrap`;
- если push завершился конфликтами, общий цикл возвращает `ConflictsDetected`, но changes все равно выполняется.

Recovery для `RequiresBootstrap` не должен молча перетирать локальные данные. UI предлагает два явных направления:
- `Отправить мои данные на сервер`: устройство считается источником первичных данных, Android создает или связывает серверного питомца, пересобирает outbox из локальных `event_types`, `pet_events`, `weight_entries` и отправляет их на сервер;
- `Заменить данными с сервера`: сервер считается источником истины, локальный outbox очищается, затем выполняется full bootstrap.

Кнопка замены серверной версией должна показывать предупреждение о потере локальных неотправленных изменений.

Кнопка отправки данных с устройства не должна выполнять full bootstrap как fallback. Если локальных записей для отправки нет, она может только связать локального питомца с серверным и сбросить флаг `requiresBootstrap`; загрузка server snapshot остается отдельным явным действием пользователя.

## Session и auth UX
`SyncSessionStore` на DataStore Preferences хранит:
- `baseUrl`
- `accessToken`
- `refreshToken`
- `userId`
- `email`
- `displayName`

Экран настроек выступает как минимальный экран подключения: адрес сервера, email, пароль, login/register.

Если sync получает `404 current_pet_not_found`, Android трактует это как отдельное состояние: у аккаунта пока нет серверного питомца. В этом состоянии пользователь выбирает одно из действий:
- отправить локальные данные на сервер и создать нового серверного питомца;
- принять код приглашения и затем загрузить данные общего питомца с сервера.

Кнопку загрузки данных с сервера в состоянии отсутствующего серверного питомца показывать не нужно: серверу нечего отдавать до создания питомца или принятия приглашения.

Logout/reset очищает session store; foreground/background sync молчат до нового входа.

## Foreground и background sync
Foreground-trigger:
- `MainActivity.onStart()` вызывает `AppSyncRunner.triggerForegroundSync()`;
- есть cooldown 30 секунд;
- есть защита от параллельных запусков;
- без сохраненной session auto-sync не стартует.

Background sync:
- один `SyncWorker` на WorkManager;
- unique periodic work `ivi-background-sync`;
- network constraint `CONNECTED`;
- использует тот же `RunFullSyncUseCase`;
- foreground/manual/background sync используют общий `SyncExecutionGate`.

## Conflict UX V1
Conflict UX построен вокруг:
- локальной таблицы `sync_conflicts`;
- экрана конфликтов из секции синхронизации;
- действий `Принять серверную версию` и `Повторить мои изменения`.

Повторная отправка пересобирает новую mutation из текущего локального состояния с `baseVersion = serverVersion`, а не переиспользует старую failed outbox-запись.

На главном экране есть мягкая карточка конфликтов, которая показывается только при `conflictCount > 0` и ведет на экран конфликтов.
