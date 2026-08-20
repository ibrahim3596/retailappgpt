# RetailPOS V2 — Repository Audit V36

Date: 2026-08-20
Branch: `retailpos-v2`

## Verified state

- Room schema: 25
- Current active build toolchain: AGP 8.13.2, Kotlin/Compose 2.2.10, KSP 2.2.10-2.0.2, Java 17.
- Staff authentication, local roles and discount/price permission rules exist.
- Checkout is transactional and persists pricing/payment/COGS snapshots.
- Encrypted database backup exists with schema/store/checksum validation.
- Intelligent capture and barcode scanning use CameraX/ML Kit.

## Fixes applied in this audit

- Restored encrypted backup is now opened and checked with SQLite `user_version` and `quick_check` before the previous database is deleted.
- Authenticated staff session is preserved across the StaffGateActivity → MainActivity handoff.
- Staff login no longer performs authentication through a keyed Compose `LaunchedEffect`; the submit action runs in a coroutine and disables inputs while active.
- Inventory adjustment and stock receiving screens now enforce the inventory permission at the feature boundary.
- Intelligent capture ML Kit callbacks now synchronize per-frame completion/results using atomic state and close each `ImageProxy` exactly once after all three analyses finish.
- Duplicate capture imports were removed.
- Temporary/stale root build artifacts used during earlier verification were removed.

## High-risk items still requiring work

- MainActivity remains a large orchestration point and still contains direct database/operation calls.
- Held-bill restore populates the in-memory cart before consuming the held-bill row; a race where another operation consumes the same held bill can leave a resumed cart with an error. This should be made an atomic claim/restore operation.
- Inventory authorization is now enforced by the adjustment/receiving screens, but the mutation callbacks in MainActivity should eventually move behind a repository/use-case boundary that accepts an explicit staff role.
- Barcode and intelligent-capture camera lifecycles require device testing; source-level lifecycle leaks addressed in this audit are not a substitute for real camera-device verification.
- No Gradle wrapper is currently present, so fully reproducible local build/test execution from a clean checkout is still not demonstrated.
- The repository contains a legacy `docs/DEVELOPER_HANDOFF.md` that describes an older Room v11 state. It must be synchronized with the current v25 source-of-truth state.
- The tracked `tmp/` directory contains historical CI/verification marker files and should be removed once all remaining filenames are enumerated.

## Validation

CI was intentionally not run. Validation in this audit is based on source inspection and targeted test/code consistency checks. A successful build/device-test campaign remains required before release.
