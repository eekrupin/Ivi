# Runbook-и и ручные проверки

## Android smoke против backend в Windows + WSL

В WSL запустить backend:

```bash
docker compose -f ./backend/docker-compose.yml up -d
./backend/gradlew -p ./backend run
```

Проверить health внутри WSL:

```bash
curl http://127.0.0.1:8080/health
```

Проверить bind внутри WSL:

```bash
ss -lntp | grep 8080
```

Ожидается `*:8080` или `0.0.0.0:8080`.

Если Windows открывает `http://localhost:8080/health`, но не открывает `http://127.0.0.1:8080/health`, настроить portproxy в PowerShell от администратора:

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

После этого Windows должен открывать:

```text
http://127.0.0.1:8080/health
```

Выполнить `adb reverse` через Windows Android SDK:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 reverse tcp:8080 tcp:8080
```

`emulator-5554` заменить на имя устройства из `adb devices`.

В Android-эмуляторе после `adb reverse` использовать backend URL:

```text
http://127.0.0.1:8080
```

Альтернатива без `adb reverse` для стандартного эмулятора:

```text
http://10.0.2.2:8080
```

Cleartext HTTP разрешен только в debug-сборке через `android/app/src/debug/AndroidManifest.xml`.

## Backend smoke/E2E

Поднять PostgreSQL:

```bash
docker compose -f ./backend/docker-compose.yml up -d
```

Запустить backend:

```bash
./backend/gradlew -p ./backend run
```

Запустить smoke:

```bash
node ./backend/e2e-smoke.mjs
```

Проверяемые сценарии:
- `GET /health`
- register/login/bad login;
- create pet;
- invite create/accept;
- bootstrap/changes/push;
- `VERSION_MISMATCH`;
- `INVALID_REFERENCE`;
- retry after conflict;
- refresh token rotation.

Отчет: `/tmp/ivi-e2e-report.json`.

## Android device-level smoke

Перед полным smoke собрать и установить debug APK на оба устройства:

```bash
./android/gradlew -p ./android :app:assembleDebug
/home/ekrupin/bin/wadb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk
/home/ekrupin/bin/wadb -s emulator-5556 install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Быстрая проверка, что Compose `testTag` доступны внешнему UIAutomator как `resource-id`:

```bash
node scripts/android-two-device-smoke.mjs emulator-5554 emulator-5556
```

Скрипт использует `resource-id` из `IviTestTags`, сам настраивает `adb reverse tcp:8080 tcp:8080`, запускает приложение и проверяет базовые selectors на двух устройствах. Он не использует жёстко заданные координаты; tap выполняется по bounds найденного UIAutomator node.

Последний ручной two-device smoke выполнялся на `emulator-5554` + `emulator-5556` через `http://127.0.0.1:8080` и `adb reverse tcp:8080 tcp:8080`. Через UI/test tags проверены базовая синхронизация A, invite accept, owner transfer B, single-owner delete C, conflict flow D, оба resolver-действия `принять серверную версию` и `повторить мои изменения`, logout/reconnect и member leave. Photo sync через системный photo picker проверен пользователем туда и обратно; после наблюдения частично отображенного фото при плохой сети Android photo storage усилен атомарной записью через temporary file + move и проверкой пустого download body. Дополнительно исправлен stale local `photoUri`: если после server snapshot pet уже `SYNCED`, скачанное remote-фото заменяет старый локальный `pet_*.jpg` в БД.

Для чистого smoke перед запуском можно очистить локальные данные приложения:

```bash
/home/ekrupin/bin/wadb -s emulator-5554 shell pm clear ru.ekrupin.ivi
/home/ekrupin/bin/wadb -s emulator-5556 shell pm clear ru.ekrupin.ivi
```

После установки debug APK поверх текущей базы проверить:
- запуск приложения без Room migration crash;
- переход на экран настроек/напоминаний без DataStore crash;
- login/register UX;
- ручной sync;
- foreground sync после возврата в приложение;
- invite visibility;
- карточку конфликтов на главном экране;
- экран конфликтов;
- действия resolver: принять серверную версию и повторить мои изменения.

Для smoke через эмуляторы использовать backend URL `http://127.0.0.1:8080` после `adb reverse tcp:8080 tcp:8080`. В одном проходе `http://10.0.2.2:8080` давал timeout на manual sync, поэтому для воспроизводимой проверки предпочтителен reverse.

Ключевые selectors для ручного two-device smoke:
- Auth/session: `settings_sync_base_url_field`, `settings_sync_email_field`, `settings_sync_display_name_field`, `settings_sync_password_field`, `settings_login_button`, `settings_register_button`, `settings_logout_button`, `settings_connection_status`.
- Home: `home_root`, `home_pet_overview`, `home_pet_photo`, `home_edit_pet_button`, `home_conflict_card`, `home_open_conflicts_button`.
- Sync/settings: `settings_root`, `settings_sync_section`, `settings_manual_sync_button`, `settings_no_server_pet_card`, `settings_publish_local_button`, `settings_conflict_card`, `settings_open_conflicts_button`.
- Invite/leave: `invite_section`, `pet_access_status`, `create_invite_button`, `invite_code_text`, `accept_invite_code_field`, `accept_invite_button`, `leave_pet_button`, `transfer_owner_button_<userId>`, `delete_pet_confirm_button`.
- Domain/photo/conflicts: `event_type_add_button`, `event_type_name_field`, `event_type_save_button`, `event_create_button`, `event_comment_field`, `event_save_button`, `weight_add_button`, `weight_value_field`, `weight_save_button`, `pet_edit_photo_area`, `pet_edit_change_photo_button`, `pet_edit_remove_photo_button`, `pet_edit_save_button`, `conflicts_root`, `conflict_item_<id>`, `conflict_accept_server_button_<id>`, `conflict_retry_local_button_<id>`.

APK debug:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Если после изменений Hilt/DI или конструктора `@HiltViewModel` приложение падает с `NoSuchMethodError` в `DaggerIviApplication_HiltComponents_*`, не ставить APK из обычной инкрементальной сборки. Сначала выполнить clean-сборку:

```bash
./android/gradlew -p ./android :app:clean :app:assembleDebug
```

Причина: D8/Gradle incremental cache может собрать APK с несогласованными generated Hilt dex-классами. Удаление приложения на устройстве не исправляет такой APK; нужен свежий clean APK.

## Проверка после Android-изменений

Базовый порядок:

```bash
./android/gradlew -p ./android :app:assembleDebug
./android/gradlew -p ./android test
./android/gradlew -p ./android :app:lintDebug
```

Если задача точечная и пользователь явно просит только debug APK, достаточно:

```bash
./android/gradlew -p ./android :app:assembleDebug
```

## Проверка после backend-изменений

```bash
./backend/gradlew -p ./backend build
```

Для runtime-проверки:

```bash
docker compose -f ./backend/docker-compose.yml up -d
./backend/gradlew -p ./backend run
curl http://127.0.0.1:8080/health
```
