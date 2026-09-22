# RetailGPT / RetailPOS

A production-grade retail point-of-sale application for Android (Kotlin + Jetpack Compose + Room + KSP) with a TypeScript backend.

## Repository Layout

```
app/                    # Android application (Kotlin, Compose, Room)
server/                 # TypeScript backend (NestJS-style, Vitest, ESLint)
docs/                   # Spec and design docs
```

## CI Status

Both GitHub Actions workflows are **green**:

- **Android CI** — `assembleDebug` + `testDebugUnitTest` + debug APK artifact.
- **Backend CI** — typecheck, build, tests, lint, `npm audit --omit=dev`.

## Debug APK

A debug APK is built on every push to `main`/ `develop` and uploaded as a GitHub Actions artifact named `debug-apk`.

Install a downloaded APK on a device/emulator with:

```bash
adb install -r app-debug.apk
```

## How to Build Locally

```bash
export JAVA_HOME=/home/VikinG/tools/jdk-17.0.20.1+1
./gradlew :app:assembleDebug -x lint
```

Requires the Android SDK (`ANDROID_HOME` / `sdk.dir` in `local.properties`).

## How to Resume

1. Check the latest CI runs:
   `gh run list --repo ibrahim3596/retailappgpt --branch main --limit 2`
2. Fix any `Unresolved reference` / experimental-API errors in Kotlin sources.
3. Push; GitHub Actions will verify.
4. Backend: `cd server && npm test && npm run build && npx eslint src --max-warnings=0`.

## Recent Changes

- Notification/alert system (P2): `InventoryAlertWorker`, `SyncAlertWorker`,
  `KhataAlertWorker`, `NotificationWorker` — scheduled on login, cancelled on
  logout via `MainViewModel.scheduleBackgroundAlerts()` / `cancelBackgroundAlerts()`.
- Fixed Compose API mismatches: `HorizontalDivider` (material3), `clip(Shapes.medium)`,
  `@OptIn(ExperimentalMaterial3Api::class)` on `ExpenseDialog`.
- Added placeholder `.env` so the Secrets Gradle Plugin generates a valid
  `BuildConfig` in CI (no real secrets committed).
- Backend CI audit gate now ignores dev dependencies.