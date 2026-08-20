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

2. **Billing-speed foundation**
   - Existing Room favorite table was found to have an entity but no DAO.
   - Added `FavoriteProductDao` and exposed it through `RetailDatabase`.
   - This makes persistent favorite state a real Room path rather than only the old process-local store.
   - Full POS visual quick-add integration should still be completed after the DAO path is wired through the app shell.

3. **Returns/refunds hardening**
   - Credit-sale returns now require `CREDIT_REVERSAL`.
   - Non-credit returns cannot use `CREDIT_REVERSAL`.
   - Added unit coverage for the refund-method rules.

4. **Staff/inventory controls**
   - Inventory adjustments, batch adjustments, and stock receiving now enforce `StaffPermission.ADJUST_INVENTORY` inside `InventoryDao`.
   - Permission enforcement is therefore at the data/business boundary, not only UI level.
   - Inventory valuation and near-expiry DAO queries remain present and are preserved.

5. **Checkout recovery**
   - `PendingPaymentStore` now persists entered cash/tender data using SharedPreferences.
   - It is initialized from `RetailDatabase.get(context)`.
   - This improves process-recreation resilience for pending checkout tender data.
   - Full persistent checkout-idempotency recovery is still a separate hardening task because the in-memory idempotency key lives in the app shell.

## Current known follow-ups

- Wire `FavoriteProductDao` into `PosScreen`/`MainActivity` for visible persistent favorites/recent quick-add.
- Add item-level discount and price-override UI with `StaffPermission` enforcement.
- Add report/settings/product navigation permission guards where currently only the underlying action is protected.
- Complete split-payment analytics normalization so split sales report by tender components rather than as raw encoded paymentMethod strings.
- Complete full physical-device and integration testing only after development freeze.

## No CI

GitHub Actions remain deliberately unused during this period. Never claim CI-green without a successful workflow result for the exact commit.
