# API Contract

`api/` хранит первый версионируемый контракт backend API и синхронизации проекта «Иви».

Source of truth:

- TypeSpec-исходники в `src/main.tsp`

Производный артефакт:

- OpenAPI 3.1 в `generated/openapi/openapi.yaml`

## Структура

- `src/main.tsp` — основной контракт API и sync-модели
- `docs/sync.md` — краткие правила синхронизации и ограничения первой версии
- `tspconfig.yaml` — конфигурация TypeSpec/OpenAPI emitter
- `package.json` — локальные команды для генерации контракта
- `generated/openapi/openapi.yaml` — сгенерированный OpenAPI-артефакт

## Команды

Установить зависимости:

```bash
npm install
```

Сгенерировать OpenAPI:

```bash
npm run generate:openapi
```

Проверить, что контракт компилируется:

```bash
npm run build
```

## Что входит в первую версию

- auth: register, login, refresh
- me
- pets/current
- invites
- sync/bootstrap
- sync/changes
- sync/push
- photo upload/delete
- health

## Что пока intentionally rough

- финальный lifecycle `refreshToken` и правила ротации токенов
- точная форма binary upload для фото питомца на уровне production-ограничений
- лимиты пагинации и максимальный размер одной `push`-пачки
- полный набор бизнес-кодов ошибок для всех сценариев

Эти места уже зафиксированы по смыслу в контракте, но будут уточняться при разработке `backend/`.
