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
