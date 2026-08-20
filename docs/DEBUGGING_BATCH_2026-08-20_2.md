# Debugging Batch — 2026-08-20 (2)

## Completed

- Wired `ActiveCartStore` into application startup and `RetailPosApp` so the active cart survives process death.
- Added `CheckoutRecoveryFingerprint` to bind recoverable checkout state to product, quantity, price, override and item-discount state.
- Updated `PendingPaymentStore` so cash/idempotency state cannot silently migrate to a different cart.
- Updated `SaleDao.checkout()` to consume pending cash only when the cart fingerprint matches.
- Removed the stale `inventoryDao` argument from the `AnalyticsScreen` call in `MainActivity`, matching the actual screen signature.
- Added unit coverage for checkout fingerprint stability, quantity/price/discount changes and line-order independence.
- Previous batch: analytics now subtracts return revenue, returned quantities and restored cost from daily net metrics.
- Previous batch: Settings loads GST mode, default tax rate and merchant UPI VPA from Room after database restore.
- Previous batch: scanner/capture lifecycle and held-bill claim races were hardened.

## Recovery invariant

A pending cash amount or idempotency key may only be reused when its SHA-256 cart fingerprint matches the currently recovered cart.

Changing quantity, price override, item discount or product identity changes the fingerprint and therefore invalidates stale checkout recovery state.

## Validation

Static cross-file inspection and focused unit-test additions performed.

GitHub Actions were not run.

A full Gradle/device build remains unverified in the current environment.
