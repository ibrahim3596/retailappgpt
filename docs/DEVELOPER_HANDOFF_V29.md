# RetailPOS V2 — Authoritative Developer Handoff (v29)

**Repository:** `ibrahim3596/retailappgpt`
**Branch:** `retailpos-v2`
**Date:** 2026-08-20
**Room version:** 24

## Latest development slice

### Durable active bill
- Added `ActiveCartStore` using application-scoped SharedPreferences JSON storage.
- `RetailDatabase` configures the store without clearing existing data.
- `CartManager` now loads the previous active cart on construction.
- Every successful add, quantity change, pricing edit/replace, and removal is persisted automatically.
- Clear removes the durable snapshot.
- Item-level selling-price overrides and item discounts are preserved.
- No Room schema migration was required.

### Existing recovery foundations preserved
- Pending checkout tender is persisted.
- Checkout idempotency key is persisted.
- Held bills are persisted in Room and taken transactionally.
- Split payments are encoded and normalized for reporting.

## Remaining offline/recovery work
- Validate restored active-cart lines against current product existence, archive state, and available stock before presenting them as sellable.
- Preserve an explicit recovery warning when a restored line is no longer sellable rather than silently changing price/quantity.
- Add full process-death integration coverage after the development freeze.
- Test database corruption/reopen, migration continuity, interrupted checkout, repeated checkout, and offline restart on a physical device.

## Other current status
- Staff navigation and secondary activity permission gates are implemented.
- Indian voice support remains Hindi-first with selectable Indian languages and model-download support where Android provides it.
- Core POS, Product Master, inventory, purchasing, Khata, returns, expenses, analytics, UPI, split payments, and intelligent capture foundations are implemented.

## Verification status
- No CI workflow has been triggered for this checkpoint.
- No claim of compile-green/device-green is made.
- Comprehensive build, integration, physical-device, offline, migration, security, and release QA remain scheduled for the development freeze phase.
