# Roadmap и текущий статус

## Стадия
Локальный Android MVP стабилизирован, идет архитектурный этап backend и синхронизации.

## Уже определено
- Android-клиент живет в `android/`.
- Backend живет в `backend/`.
- API-контракт живет в `api/`.
- Backend — Kotlin/Ktor/PostgreSQL/Flyway/Exposed DSL.
- Серверная среда: Ubuntu mini server.
- NAS не используется как primary storage.
- Один текущий питомец в MVP; первый серверный сценарий ориентирован на двух пользователей, но membership/ownership не должен архитектурно зависеть от жесткого максимума два участника.

## Уже реализовано крупными блоками
- Локальный Android MVP на Room.
- Реальные Android CRUD-сценарии для питомца, типов событий, событий, веса и напоминаний.
- Локальные уведомления через AlarmManager.
- Фото питомца через Android Photo Picker.
- API TypeSpec и generated OpenAPI.
- Backend skeleton и реальные DB-backed auth/pet/invite endpoints.
- Backend sync endpoints: bootstrap, changes, push.
- Android sync read path, push path, app-level orchestration.
- Session/auth UX через DataStore-backed `SyncSessionStore`.
- Foreground и background sync foundation.
- Conflict UX V1 и entrypoint на главном экране.
- Backend smoke/E2E script.
- Device-level fixes для Room migrations и DataStore singleton.
- Debug-only cleartext HTTP и сетевые permissions для Android local smoke.

## Что в работе
- Доведение device-level Android E2E: вход, sync, invite visibility, conflict card, conflict resolver.
- Усиление реальной клиент-серверной интеграции sync.
- Полировка текстов/причин конфликтов.
- Усиление тестов conflict flow и background orchestration.

## Следующие шаги
1. Добить device-level Android прогон на живом устройстве/эмуляторе.
2. Проверить ручной сценарий входа и sync против локального backend.
3. Проверить invite visibility и общий доступ второго пользователя.
4. Проверить owner transfer/delete при выходе владельца в device-level E2E.
5. Проверить conflict card и conflict resolver.
6. После smoke/E2E прохода итеративно усилить тесты conflict flow, тексты и background orchestration.
7. Следующим большим структурным этапом добавить `infra/` без переноса Android и без ручного переопределения API-контракта.

## Открытые вопросы
- Нужна ли синхронизация фото питомца в первой серверной поставке или вынести ее во вторую backend-итерацию.
- Насколько богатым должен стать production-grade auth/session UX после V1 screen в настройках.
- Когда вводить более устойчивую retry-машину для background sync.

## Что откладывается
- NAS backup.
- Дополнительные аналитические экраны.
- Вложения и документы.
- Web-клиент.
- Несколько питомцев.
- Server-side push-уведомления.
- Сложная ролевая модель.
