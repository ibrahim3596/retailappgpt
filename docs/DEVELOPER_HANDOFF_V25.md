# RetailPOS V2 — Authoritative Developer Handoff (v25)

**Repository:** `ibrahim3596/retailappgpt`
**Branch:** `retailpos-v2`
**Date:** 2026-08-20
**Current Room version:** 24

## Development mode

The user chose: finish core feature development first, then freeze scope and run comprehensive build/device testing. Do not trigger GitHub Actions while the user's Actions budget is exhausted.

## Completed in the latest development batch

1. **Split payments**
   - Added `SplitPaymentPart` and `SplitPaymentRules`.
   - Cash + UPI or Cash + Card split settlement is validated exactly against the bill total.
   - Split payment is encoded into the existing sale `paymentMethod` field, so no schema migration was required.
   - Checkout UI supports split tender entry.
   - Added unit tests for validation and encode/decode round trip.

2. **Billing-speed / persistent quick-add**
   - Added `FavoriteProductDao` and exposed it through `RetailDatabase`.
   - `PosScreen` now reads store-scoped favorites from Room.
   - `PosScreen` rebuilds a recently-sold quick-add list from recent sale lines.
   - Favorite toggling is persisted to Room and survives process recreation.
   - Existing quick-add rules and UI are reused rather than duplicated.

3. **Returns/refunds hardening**
   - Credit-sale returns now require `CREDIT_REVERSAL`.
   - Non-credit returns cannot use `CREDIT_REVERSAL`.
   - Added unit coverage for the refund-method rules.

4. **Staff/inventory controls**
   - Added explicit `StaffPermission.ADJUST_INVENTORY`.
   - Inventory adjustments, batch adjustments, and stock receiving enforce it inside `InventoryDao`.
   - Added item-level discount authorization rules: owner 100%, manager 30%, cashier denied.
   - Added unit coverage for the new item-level discount rules.

5. **Checkout recovery**
   - `PendingPaymentStore` now persists entered cash/tender data using SharedPreferences.
   - It is initialized from `RetailDatabase.get(context)`.
   - Full persistent checkout-idempotency recovery is still a separate hardening task because the app-shell idempotency key remains in memory.

## Deferred in this batch

- Split-payment analytics normalization was attempted, but the live `SaleDao.kt` blob reported a stale SHA conflict. No forced overwrite was made.
- Item-level discount and selling-price override UI still need to be wired into the POS cart UI and checkout pricing path.
- Navigation-level permission guards still need to be added where screens expose owner/manager-only actions.
- Full persistent checkout-idempotency recovery across process death remains outstanding.

## Verification status

No local Android/Gradle build has been run after the latest batch. Do not describe the current branch as compile-verified or CI-green.

## No CI

GitHub Actions remain deliberately unused during this period. Never claim CI-green without a successful workflow result for the exact commit.
