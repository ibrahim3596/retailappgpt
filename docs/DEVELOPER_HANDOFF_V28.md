# RetailPOS V2 — Authoritative Developer Handoff (v28)

**Repository:** `ibrahim3596/retailappgpt`
**Branch:** `retailpos-v2`
**Date:** 2026-08-20
**Current Room version:** 24

## Development mode

Finish feature development first, then freeze scope and run the complete build/device/regression campaign. GitHub Actions remain deliberately unused while the user's Actions budget is exhausted.

## Completed in this development pass

### Permission hardening
- Added `StaffPermission.MANAGE_EXPENSES`.
- Added navigation helpers for expenses and returns.
- Purchase activity now refuses entry when the active staff role cannot manage inventory.
- Return activity now refuses entry when the active staff role cannot process returns.
- Expense activity now refuses entry when the active staff role cannot manage expenses.
- Home dashboard now hides Purchase, Return, and Expense actions unless the role is authorized.
- Existing Products/Inventory/Analytics/Settings dashboard visibility remains role-aware.

### Voice / Indian languages
- Existing voice stack already supports Hindi as default plus English (India), Telugu, Malayalam, Marathi, Tamil, Kannada, Bengali, Gujarati, Punjabi, and Odia.
- Android 13+ voice model status/download support exists in the voice UI.
- Added multilingual parser regression tests for Hindi, Telugu, Malayalam, Marathi, loose-weight quantities, and kg-to-gram conversion.

### Earlier completed foundations preserved
- Persistent favorites/recent quick-add foundation.
- Split payments and normalized payment reporting.
- Item-level discount and selling-price override with staff validation.
- Persistent pending cash/tender and checkout idempotency key.
- Product archive/restore and checkout hard-stop.
- Inventory valuation and near-expiry exposure.
- Expenses, analytics, UPI handoff, purchasing, returns, Khata, intelligent capture.

## Remaining major development gaps

- Active cart persistence across process death is not yet fully wired; held bills are durable, but the currently open unsaved cart can still be lost on a hard process kill.
- Full navigation guards inside every possible secondary route should be reviewed again after the activity gates.
- Split-payment normalization exists in dashboard/analytics, but database-level aggregate payment queries still return encoded split strings; this is acceptable for now because UI normalization handles it.
- Full offline reliability campaign, migration testing, process-death testing, device testing, performance testing, accessibility testing, and release hardening remain deferred until feature freeze.
- Cloud backup/synchronization remains intentionally out of scope for the current offline-first completion pass.

## Verification state

No full Android/Gradle build or CI verification has been performed after this batch. Do not call the branch compile-verified or CI-green.
