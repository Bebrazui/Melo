# Melo

Нативный Android-музыкальный сервис с экстракцией треков **на устройстве** (yt-dlp локально) и
дополнительным community-каталогом. Open-source, дистрибуция через F-Droid.

> ⚠️ Юридически серая зона. Проект использует yt-dlp для доступа к контенту сторонних платформ.
> Используйте на свой риск и в соответствии с законодательством вашей страны.

---

## Идея

Вместо центрального стрим-сервера (который YouTube быстро блокирует по IP и который дорог по трафику)
**экстракция и воспроизведение происходят прямо на телефоне**. Каждый пользователь работает со своего
IP — это путь NewPipe / Seal / LibreTube, дружелюбный к F-Droid.

```
┌─────────────────────── ANDROID (Kotlin + Compose) ───────────────────────┐
│  FREE / FOSS / on-device (работает без сервера):                          │
│   • youtubedl-android (yt-dlp + Python, обновление бинаря в рантайме)     │
│   • WebView-логин → CookieManager → cookies.txt → yt-dlp                  │
│   • Media3 / ExoPlayer + MediaSessionService (фон, экран блокировки)      │
│   • Room (плейлисты, лайки, история, оффлайн-загрузки)                    │
│   • Чтение community-каталога с GitHub (catalog.json + Releases)          │
│                                                                          │
│  PREMIUM (требует сервер + подписку):                                     │
│   • синхрон плейлистов · рекомендации · облачный бэкап                    │
└────────────────────────┬─────────────────────────────────────────────-──┘
              ┌───────────┴────────────┐
              ▼                         ▼
   ┌────────────────────┐   ┌──────────────────────────────┐
   │ GitHub (статика)    │   │ Backend (premium-only)        │
   │ catalog.json +      │   │ FastAPI + Postgres + JWT       │
   │ аудио в Releases    │   │ /sync /recommend /backup       │
   └────────────────────┘   └──────────────────────────────┘
```

**Почему так:** бесплатное приложение самодостаточно (хорошо для F-Droid), а сервер — чистый
value-add и единственная точка, которой держится подписка.

## Источники контента

1. **yt-dlp на устройстве** — YouTube Music, SoundCloud, Яндекс.Музыка и др.
2. **Community-каталог** — аудио + метаданные, присланные через форму, после ручной модерации
   попадают в GitHub-репо (`catalog.json` + Releases).

## Технологический стек

| Слой               | Технология                                           |
|--------------------|------------------------------------------------------|
| UI                 | Jetpack Compose + Material 3                          |
| Плеер / фон        | Media3 (ExoPlayer) + MediaSessionService             |
| Экстракция         | youtubedl-android (yt-dlp + Python), self-update      |
| Аутентификация     | WebView-логин → cookies                              |
| Локальная БД       | Room                                                  |
| DI                 | Hilt                                                  |
| Backend (премиум)  | FastAPI + Postgres + JWT                              |
| Каталог            | GitHub (catalog.json + Releases)                     |

## Структура репозитория (план)

```
Melo/
├── app/                 # Android-приложение (Kotlin + Compose)
├── server/              # FastAPI: премиум-фичи (sync / recommend / backup)
├── catalog/             # community-каталог: схема catalog.json + инструменты модерации
└── docs/                # API-контракт, схемы данных
```

## Сборка (без Android Studio)

Тулчейн находится в `D:\android-tools` (см. `ANDROID-CLI-README.md` там же).

```bat
:: активировать окружение для текущего окна
D:\android-tools\android-env.cmd

:: собрать debug APK -> app\build\outputs\apk\debug\
gradlew assembleDebug

:: установить на подключённое устройство (adb)
gradlew installDebug
```

Требования: JDK 17, Gradle 8.7, Android SDK (API 34/35), build-tools 35.0.0 — всё есть в тулчейне.

## Дорожная карта

- **Фаза 1 (MVP):** on-device yt-dlp (YouTube/YT Music) + чтение community-каталога. Поиск →
  резолв → фоновое воспроизведение → лайки/плейлисты (Room) → оффлайн-загрузка. WebView-логин.
- **Фаза 2:** подписка + серверные премиум-фичи, SoundCloud.
- **Фаза 3:** Яндекс.Музыка, пайплайн формы+модерации, релиз в F-Droid.

## Статус

🚧 Ранний скелет. Текущий шаг — каркас Android-приложения (Compose + Media3), затем интеграция
youtubedl-android.

## Лицензия

TBD (под F-Droid требуется FOSS-лицензия, напр. GPLv3).
