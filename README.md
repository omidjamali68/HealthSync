# HealthSync — Android Health Connect Sync App

A production-grade native Android application (Kotlin) that reads health data from
**Android Health Connect** (heart rate samples + daily step count) and periodically
syncs it to a configurable REST API endpoint over HTTPS.

## Features

- Native Kotlin, **MVVM + Repository** architecture.
- **Jetpack Compose + Material 3** UI.
- **Health Connect SDK** integration (heart rate, steps).
- Runtime permission flow with rationale screen.
- **WorkManager** periodic background sync (configurable interval).
- **Retrofit + OkHttp** for HTTPS JSON POST, with retry/backoff.
- **EncryptedSharedPreferences** (Jetpack Security) for API token + config.
- **Room** offline queue for unsynced batches with retry logic.
- **Hilt** dependency injection.
- Screens:
  1. Onboarding
  2. Health Connect permission setup
  3. Settings / API configuration (base URL, token, sync interval, device ID)
  4. Sync status dashboard (last sync, queue size, manual sync, errors)
- Targets **Android 10+ (minSdk 29)**; Health Connect available on Android 8+ via Play Store,
  but the SDK officially supports 9+. We use 29 to keep modern foreground service rules sane.

## Project layout

```
app/src/main/java/com/example/healthsync/
  data/
    local/       Room DB, DAOs, encrypted prefs
    remote/      Retrofit API, DTOs
    repository/  HealthRepository, ConfigRepository, SyncQueueRepository
  domain/model/  Domain models (HeartRateSample, StepRecord, SyncBatch)
  di/            Hilt modules
  sync/          SyncWorker, SyncScheduler
  ui/
    onboarding/  OnboardingScreen + VM
    permissions/ PermissionsScreen + VM
    settings/    SettingsScreen + VM
    dashboard/   DashboardScreen + VM
    theme/       Material 3 theme
  util/          Helpers, constants
```

## Build & run

1. Open project in **Android Studio Iguana (or newer)**.
2. Ensure JDK 17.
3. Install the **Health Connect** app from Play Store on the test device
   (pre-installed on Android 14+).
4. Sync Gradle, then `./gradlew :app:installDebug`.
5. Launch, complete onboarding, grant Health Connect permissions, configure API.

## API configuration (Settings screen)

| Field           | Example                              |
|-----------------|--------------------------------------|
| Base URL        | `https://api.example.com/`           |
| Auth token      | `Bearer eyJhbGciOi...`                |
| Sync interval   | `15` minutes (min 15, WorkManager)   |
| Device ID       | Auto-generated UUID, editable        |

Stored in `EncryptedSharedPreferences` (`AES256_GCM` value, `AES256_SIV` key).

## Sample API payload

`POST {baseUrl}/v1/health/ingest`
Headers: `Authorization: Bearer <token>`, `Content-Type: application/json`

```json
{
  "deviceId": "8f3b...-uuid",
  "syncedAt": "2026-06-07T10:24:00Z",
  "window": { "from": "2026-06-07T09:00:00Z", "to": "2026-06-07T10:24:00Z" },
  "steps": [
    { "date": "2026-06-07", "count": 4321 }
  ],
  "heartRate": [
    { "timestamp": "2026-06-07T10:11:23Z", "bpm": 72 },
    { "timestamp": "2026-06-07T10:12:23Z", "bpm": 74 }
  ]
}
```

Expected response: `200 OK` with `{ "accepted": true }`. Any non-2xx triggers retry
with exponential backoff (WorkManager) and the batch stays in the Room queue.

## Environment config

`local.properties` (not committed):
```
DEFAULT_BASE_URL=https://api.example.com/
DEFAULT_INGEST_PATH=v1/health/ingest
```
These are exposed via `buildConfigField` in `app/build.gradle.kts` and act only as
**initial defaults** — the user can override in Settings.

## Permissions

Declared in `AndroidManifest.xml`:
- `android.permission.health.READ_HEART_RATE`
- `android.permission.health.READ_STEPS`
- `android.permission.POST_NOTIFICATIONS` (sync status notification, 13+)
- Health Connect privacy policy intent filter.

## Security notes

- API token never logged.
- TLS only (cleartext disabled in `network_security_config.xml`).
- Encrypted storage at rest.
- Health data leaves the device **only** to the user-configured endpoint.

## License

MIT — see project header comments.
