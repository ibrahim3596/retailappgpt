# RetailPOS V2 — Authoritative Developer Handoff (v26)

**Repository:** `ibrahim3596/retailappgpt`
**Branch:** `retailpos-v2`
**Date:** 2026-08-20
**Current Room version:** 24

## Development mode

Finish core feature development first, then freeze scope and run comprehensive build/device testing. Do not trigger GitHub Actions while the Actions budget is exhausted.

## Completed in this development batch

1. **Item-level pricing in active checkout**
   - `CartLine` now preserves base selling price plus optional override unit price and item discount amount.
   - `CartLinePricingRules` validates pricing changes against `StaffPermission`.
   - Manager/owner can edit permitted item pricing in checkout; cashier is blocked from item discounts and price overrides by policy.
   - Checkout UI displays base price, override price and item discount when present.
   - The live `CartManager` is updated from the checkout editor.
   - Sale transaction validates the same rules again before stock/payment mutation.
   - Item discount is applied before any bill-level discount, then tax is calculated from the final discounted taxable amount.
   - Sale line unit price records the effective selling price.

2. **Persistent checkout idempotency**
   - `PendingPaymentStore` now persists the active checkout idempotency key alongside pending tender data.
   - The active key survives process recreation.
   - A new key is created only when a new bill is started.
   - The key is cleared when a bill is completed, held, or explicitly cleared.
   - `SaleDao` continues to enforce the unique database idempotency key inside the checkout transaction.

3. **Tests**
   - Added `CartLinePricingRulesTest` covering cashier restrictions, manager/owner overrides, and discount bounds.
   - Existing split-payment, return, permission and payment tests remain part of the repository.

## Current known follow-ups

- Add navigation-level staff permission guards for reports, settings, product management, staff management and inventory actions where needed.
- Normalize split-payment analytics into component tenders instead of raw `SPLIT:...` payment-method strings.
- Finish billing quick-add UI polish and recent/favorite interaction testing.
- Harden return/refund screens and audit logging.
- Add offline/recovery coverage for process death, migration, interrupted checkout and held-bill recovery.
- Complete full physical-device and integration testing only after feature freeze.
- Cloud backup/sync remains later scope.

## Verification status

No local Android/Gradle build has been run after this batch. Do not describe the current branch as compile-verified or CI-green.

## CI constraint

GitHub Actions remain deliberately unused. Never claim CI-green without a successful workflow result for the exact commit.
