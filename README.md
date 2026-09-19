# RetailGPT / RetailPOS

A production-grade retail point-of-sale application for Android (Kotlin + Jetpack Compose + Room + KSP) with a TypeScript backend.

## Repository Layout

```
app/                    # Android application (Kotlin, Compose, Room)
server/                 # TypeScript backend (NestJS-style, Vitest, ESLint)
docs/                   # Spec and design docs
```

## Current Status

### Backend CI
- **Status:** Failing on `npm audit` (pre-existing dev-dependency advisories in `@vitest/mocker` / `esbuild` — not introduced by recent work).
- Tests and lint otherwise pass.

### Android CI
- **Status:** Failing on Kotlin compile errors in `ExpenseScreen.kt`.
- Root cause: `ExposedDropdownMenuBox` / `ExposedDropdownMenu` / `DropdownMenuItem` are experimental APIs in the project's Compose BOM (2024.09.00) and require `@OptIn(ExperimentalMaterial3Api::class)` on the composable that uses them.
- Fix in progress: added `@OptIn(ExperimentalMaterial3Api::class)` to the private `ExpenseDialog` composable.

### Known Issues / TODO
1. **`ExpenseScreen.kt`** — `ExposedDropdownMenuBox` experimental API warnings. Fix: `@OptIn(ExperimentalMaterial3Api::class)` on `ExpenseDialog` (applied).
2. **Backend `npm audit`** — 5 pre-existing vulnerabilities in dev deps (`@vitest/mocker`, `esbuild`, `vite`). Fix: `npm audit fix --force` (breaking change to vitest) or relax the audit gate in CI.
3. **Android instrumentation tests** — require an emulator; not run in standard GitHub Actions.
4. **Notification/alert system** (P2) — implemented: `InventoryAlertWorker`, `SyncAlertWorker`, `KhataAlertWorker`, `NotificationWorker`. Schedules on login, cancels on logout via `MainViewModel.scheduleBackgroundAlerts()` / `cancelBackgroundAlerts()`.

## How to Resume

1. Check the latest Android CI run log for remaining compile errors:
   `gh run list --repo ibrahim3596/retailappgpt --branch main --limit 1`
2. Fix any `Unresolved reference` / `This material API is experimental` errors.
3. Push; GitHub Actions (`Android CI` / `Backend CI`) will verify.
4. To build a debug APK locally:
   ```bash
   export JAVA_HOME=/home/VikinG/tools/jdk-17.0.20.1+1
   ./gradlew :app:assembleDebug -x lint
   ```
5. To run the backend tests:
   ```bash
   cd server && npm test
   ```

## Commit History (recent)
- `fix: add Color import to ExpenseScreen`
- `fix: use Shapes.medium directly in clip() calls`
- `fix: add clip import, resolve nullable receiver on invoiceNumber`
- `fix: resolve horizontalScroll, HorizontalDivider, and Modifier.shape issues`
- `fix: remove duplicate _analyticsRange declaration`
- Notification/alert system implementation (4 WorkManager workers + DAO methods + ViewModel integration)