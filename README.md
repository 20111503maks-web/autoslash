# AutoSlash

Локальное Android-приложение (Kotlin + Jetpack Compose), которое автоматически перелистывает
короткие видео в **TikTok** и **YouTube Shorts** на вашем устройстве через Accessibility Service.
Root не нужен, данные аккаунта не читаются.

## Архитектура

```
app/src/main/java/com/autoslash/
├── domain/      Platform (TIKTOK, YOUTUBE_SHORTS), SessionSettings
├── session/     SessionEngine — единый цикл сессии для всех платформ (таймер, лимиты,
│                случайное время просмотра, пауза, счётчик); SessionManager — мост UI ↔ сервис
├── service/     AutoScrollService (AccessibilityService), SwipeGestureFactory,
│                FeedDetector (определение Shorts), FloatingStopOverlay (плавающая кнопка STOP)
├── platform/    PlatformLauncher (открытие приложения/Shorts), AccessibilityStatus
├── data/        SettingsRepository (DataStore)
└── ui/          MainActivity + Compose: выбор платформы, настройки, статистика
```

От платформы зависят только package name, способ открытия (deep link на Shorts для YouTube)
и необходимость подтвердить, что на экране именно лента Shorts. Всё остальное общее.

**Безопасность:**
- жест выполняется только пока идёт сессия, экран включён и разблокирован, а активное окно
  принадлежит выбранной платформе (проверяется перед каждым свайпом);
- вся сессия — одна корутина: STOP, достижение лимита или отключение сервиса отменяют её,
  и фоновых циклов не остаётся; при закрытии приложения (Back) сессия тоже останавливается;
- для YouTube свайпы начинаются только когда виден Shorts-плеер (или после нажатия «I'm in Shorts»
  на плавающей панели, если YouTube изменил интерфейс);
- сервис читает только имя пакета и ID view; пароли, cookies, токены и текст не читаются.

## Сборка и установка

1. **Открыть:** Android Studio (Ladybug или новее) → *File → Open* → папка проекта. Дождитесь Gradle Sync.
2. **Собрать APK:** *Build → Build App Bundle(s) / APK(s) → Build APK(s)*, или в терминале
   `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
3. **Установить:** `adb install -r app/build/outputs/apk/debug/app-debug.apk`, или скопируйте APK
   на телефон и откройте его (разрешите установку из неизвестных источников).
4. **Включить сервис:** *Настройки → Специальные возможности → Установленные приложения →
   AutoSlash auto-scroll → Вкл.* (на Android 13+ при установке не из магазина сначала:
   *Настройки → Приложения → AutoSlash → ⋮ → Разрешить ограниченные настройки*).
5. **Запуск:** откройте AutoSlash, выберите TikTok или YouTube Shorts, настройте длительность и
   интервал, нажмите **START SESSION** — откроется выбранное приложение. Для YouTube, если открылась
   не вкладка Shorts, нажмите Shorts вручную.
6. **Остановка:** кнопка **■ STOP** на плавающей панели поверх видео, кнопка STOP в приложении,
   или выключение сервиса в настройках. Сессия также завершается сама по лимиту времени/видео.

## Тесты

`./gradlew test` — юнит-тесты `SessionEngine` (лимиты, STOP, отсутствие жестов вне приложения,
ожидание Shorts, переключение TikTok → YouTube Shorts → TikTok).
