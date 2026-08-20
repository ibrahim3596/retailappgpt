# Debugging Batch — 2026-08-20 (3)

## Checkout recovery hardening

- Wired the durable `ActiveCartStore` into `RetailPosApp` startup and cart-state persistence.
- Added `CheckoutRecoveryFingerprint` for deterministic cart identity.
- Added a separate full transaction fingerprint covering:
  - cart lines and pricing
  - payment method
  - customer
  - bill discount
- `PendingPaymentStore` now keeps cash-tender recovery keyed to the cart fingerprint and idempotency keys keyed to the full transaction fingerprint.
- `SaleDao.checkout()` only reuses persisted cash tender when the cart fingerprint matches.
- `MainActivity` now requests/reuses idempotency using the full checkout transaction fingerprint.
- Checkout restores a persisted cash tender amount when reopening after process recreation and no longer stores split-payment total as cash recovery state.
- `MainActivity` now matches the actual `AnalyticsScreen` signature; the stale `inventoryDao` argument was removed.

## Tests

Added coverage for:

- stable cart fingerprints
- fingerprint changes from quantity/price/discount changes
- line-order independence
- transaction fingerprint changes from payment method
- transaction fingerprint changes from customer
- transaction fingerprint changes from bill discount

## Validation

Static cross-file inspection and focused unit-test additions completed.

GitHub Actions were not run.

Full Gradle/device validation remains outstanding in the current environment.
