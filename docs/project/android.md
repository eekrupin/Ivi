# Android-клиент

## Состояние
Android-клиент живет в `android/` как отдельный Gradle root. Основной модуль: `:app`.

Корневой package и `applicationId`: `ru.ekrupin.ivi`.

Ключевые entrypoint-файлы:
- `android/app/src/main/java/ru/ekrupin/ivi/MainActivity.kt`
- `android/app/src/main/java/ru/ekrupin/ivi/IviApplication.kt`
- `android/app/src/main/java/ru/ekrupin/ivi/app/ui/IviAppRoot.kt`
- `android/app/src/main/java/ru/ekrupin/ivi/app/navigation/IviNavGraph.kt`
- `android/app/src/main/java/ru/ekrupin/ivi/data/local/db/IviDatabase.kt`

## Стек
- Kotlin `2.0.20`
- Android Gradle Plugin `8.5.2`
- Gradle `8.7`
- `compileSdk 35`
- `minSdk 26`
- `targetSdk 35`
- Java `17`
- Jetpack Compose
- Material 3
- Room
- Hilt
- Coroutines + Flow
- Navigation Compose
- DataStore
- WorkManager
- AlarmManager

## Архитектура
Базовое направление:
- presentation знает domain;
- data реализует контракты domain;
- domain не зависит от Android UI и Room;
- Android-специфичные детали уведомлений, БД и DataStore изолированы в data;
- Android не зависит напрямую от backend-реализации, только от API-контракта.

Use case добавлять только для сценариев с заметной бизнес-логикой, а не на каждую простую операцию.

## Реализовано
- Room как локальный source of truth.
- DAO, entity, мапперы и локальные repository-реализации.
- Debug-сидирование стартовыми данными при пустой базе; release стартует без демо-данных.
- CRUD для питомца, типов событий, событий, веса и настроек напоминаний.
- Фото питомца через системный Android Photo Picker с копированием во внутреннее хранилище.
- Фильтрация событий по `ACTIVE`, `COMPLETED`, `ARCHIVED`.
- Удаление событий из режима редактирования.
- Удаление типов событий с правилом deactivate-on-use.
- Material 3 date picker вместо ручного ввода дат.
- Foreground, manual и background sync foundation.
- Conflict UX V1 и карточка конфликтов на главном экране.
- Recovery UX для состояния `requiresBootstrap`: пользователь явно выбирает `Отправить мои данные на сервер` или `Заменить данными с сервера`.

## Room и миграции
Локальная Room-модель расширена sync-метаданными для:
- `pets`
- `event_types`
- `pet_events`
- `weight_entries`

Есть локальная таблица `sync_outbox`.

Важные решения:
- миграции `3 -> 4` и `4 -> 5` идемпотентны через проверку существования колонок перед `ALTER TABLE`;
- миграция `5 -> 6` нормализует `pets`, `event_types`, `pet_events`, `weight_entries` пересозданием таблиц по ожидаемой Room-схеме с переносом данных;
- destructive migration не использовать как способ чинить обновления поверх старой debug-базы.

## Напоминания
Механизм:
- `AlarmManager` с `setAndAllowWhileIdle`;
- `NotificationChannel`;
- `ReminderPublisherReceiver` для показа уведомлений;
- `ReminderRescheduleReceiver` для восстановления расписания после reboot, смены времени и часового пояса.

Правила:
- планируются только события с заполненной `dueDate`;
- уведомления должны быть включены у события;
- планируются только события `ACTIVE`;
- `COMPLETED` и `ARCHIVED` не планируются;
- фиксированное локальное время уведомления для MVP — `10:00`;
- разрешение `POST_NOTIFICATIONS` не запрашивается агрессивно при старте, пользователь управляет этим из настроек.

## Session и DataStore
`SyncSessionStore` хранит:
- `baseUrl`
- `accessToken`
- `refreshToken`
- минимальный профиль пользователя.

`SyncSessionStore` должен предоставляться в DI как `@Singleton`, потому что внутри создается DataStore для `sync_session.preferences_pb`. Иначе возможен crash `There are multiple DataStores active for the same file`.

## Permissions и debug networking
В `android/app/src/main/AndroidManifest.xml` должны быть:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`

Cleartext HTTP разрешен только в debug overlay:
- `android/app/src/debug/AndroidManifest.xml`
- `android:usesCleartextTraffic="true"`

Release-сборка не должна получать `usesCleartextTraffic=true`.

## Команды
Из корня репозитория:

```bash
./android/gradlew -p ./android :app:assembleDebug
./android/gradlew -p ./android test
./android/gradlew -p ./android :app:lintDebug
./android/gradlew -p ./android :app:assembleRelease
```

Если запускать wrapper из корня, обязательно указывать `-p ./android`.

При сборке есть предупреждение AGP `8.5.2` о `compileSdk 35`; оно не блокирует сборку.
