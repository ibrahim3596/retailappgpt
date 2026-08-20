# RetailPOS V2 — Authoritative Developer Handoff (v24)

**Repository:** `ibrahim3596/retailappgpt`
**Branch:** `retailpos-v2`
**Date:** 2026-08-20
**Current Room version:** 24

This file is the current portable development handoff. The repository is the source of truth; inspect actual code before editing.

## Product strategy

Build a serious Indian kirana/general-trade, shopkeeper-first, offline-first POS around three economic loops:

```text
SELL    Product → Cart → Payment → Stock ↓
BUY     Supplier → Purchase → Receive → Stock ↑ → Payable ↑
COLLECT Customer → Credit/Khata → Payment → Balance ↓
```

The flagship differentiator is Intelligent Product Capture, but core retail operations take priority over advanced AI.

## Completed major foundations

### Product Master

- Product CRUD
- SKU normalization/validation
- primary and alternate barcodes
- canonical `product_barcodes`
- GTIN/EAN/UPC validation foundation
- no-barcode products
- product search and stock filtering
- categories/subcategories
- pack size/unit
- images
- MRP, purchase price, selling price
- low-stock threshold
- product metadata
- product archive/restore
- active catalog hides archived products
- checkout rejects archived products

### Intelligent Product Capture

Implemented foundation:

- CameraX capture
- barcode + OCR + visual evidence
- multi-frame consensus
- OCR normalization and common field extraction
- MRP extraction variants
- local Product Master candidate ranking
- pack/variant matching
- local/catalog conflict resolver
- confidence/evidence/review workflow
- persistent bounded feedback history
- cache-first/offline catalog lookup
- unknown barcode → identify/add → save → active bill handoff
- retailer-controlled selling/purchase price and stock preserved

Still future/advanced:

- dedicated visual recognition service
- offline common-product vision dataset
- stronger category/unit inference
- advanced language-pack download system
- device/low-light capture hardening

### POS / Billing

- cart/search/barcode scanner
- decimal quantities
- loose-item workflow foundation
- Hindi/Hinglish voice billing foundation
- discounts
- tax/GST modes
- cash/card/UPI/credit payment records
- persistent held bills
- receipts/reprint/share foundation
- active-cart handoff from intelligent capture
- recent-sale quick-add query foundation
- persistent favorites via Room
- UPI intent/app handoff using merchant VPA

Not yet fully production-hardened:

- true split payments
- thermal printer integration
- payment confirmation with PSP/bank reconciliation

### Customers / Khata

- customer CRUD/search
- duplicate-phone protection
- credit sale
- ledger
- partial/full payment
- overpayment protection
- customer deletion blocked while balance exists
- statements/share

### Suppliers / Purchasing

- suppliers persistent
- supplier create/edit/search
- supplier balances
- supplier payments
- purchase entry
- purchase lines
- free quantity
- distributor scheme discount
- effective cost economics
- batches and expiry
- supplier payable ledger
- purchase history
- atomic purchase receiving foundation

### Inventory

- current stock
- inventory movements
- receive stock
- stock adjustments
- batch tracking
- expiry policy
- FEFO checkout
- expired-batch exclusion
- low/out-of-stock status
- batch-based valuation rules
- near-expiry and expired cost exposure on inventory detail
- COGS snapshot from actual FEFO cost allocation

Still pending:

- store-wide inventory valuation dashboard
- stock transfer/multi-location
- richer expiry dashboard

### Returns / Refunds

- recent-sale lookup
- full/partial returns
- repeated-return quantity guard
- return reason
- refund method
- staff permission enforcement
- stock restoration foundation
- original cost/batch awareness
- Khata reversal for credit returns

### Accounting / Owner view

- COGS
- gross profit
- operating result foundation
- day-end cash reconciliation
- cash/UPI/card/Khata payment mix
- customer receivables
- supplier payables
- top sellers today
- recent sales/reprint
- persistent expenses
- expense categories/payment methods
- daily expense total

### Staff / Settings

- owner/manager/cashier roles
- staff gate/PIN/session
- discount permission controls
- return permission
- store name/phone
- GST mode
- default tax rate
- receipt header/footer
- currency
- display density
- merchant UPI VPA

## Database

Room is version **24**.

Recent migrations:

- 17→18: held bills
- 18→19: suppliers/purchases/supplier ledger
- 19→20: sale cost allocations + returns
- 20→21: persistent favorites
- 21→22: product archive state
- 22→23: expenses
- 23→24: merchant UPI VPA

Do not alter entities without a migration.

## Current development mode

The user explicitly chose:

**Finish development first → freeze feature scope → run complete build/device testing → fix all failures.**

Do not restart the full QA campaign until the planned development scope is sufficiently complete.

Unit tests should still be added for new business rules.

## GitHub Actions constraint

The user previously exhausted Actions budget through repeated debugging runs.

**Do not trigger GitHub Actions during the current zero-minute period.**

Do not claim CI green without an actual successful workflow result for the exact commit.

## Local test environment prepared

The user has:

- Windows PC
- Android Studio installed but laptop is weak
- Java bundled with Android Studio (JDK 25)
- Android SDK and ADB
- spare physical Android device authorized through ADB
- Google Colab available for cloud builds/tests

The repository originally lacked `gradlew`/`gradlew.bat`. A Gradle 8.13 wrapper was generated in Colab during testing.

The first real Gradle run reached KSP and exposed missing `PaymentSummary`/`CheckoutResult` models; those were added. Comprehensive QA is intentionally deferred until development freeze.

## Critical engineering rules

- Repository is source of truth.
- Do not blindly trust older handoff text.
- Prefer coherent vertical slices.
- Do not massive-rewrite architecture for aesthetics.
- Keep business rules deterministic and testable.
- Weak intelligent-capture guesses require human review.
- QR payloads are not product identifiers.
- Retailer-controlled price/stock/SKU fields must not be silently overwritten by catalog data.
- Keep purchase/stock/payable updates transactional where they represent one economic event.
- Never use CI as the debugging loop while budget is unavailable.

## Remaining high-priority development

1. Finish POS edge cases: split payment, printer/reprint hardening, price override/item-level discount policy.
2. Complete store-wide inventory valuation and expiry dashboard.
3. Finish reports/export/owner workflows.
4. Complete offline reliability/export/backup strategy.
5. Finish advanced Intelligent Capture only after core retail operations are stable.
6. Add final polish/accessibility/performance/security hardening.
7. Freeze feature scope.
8. Run the full Colab Gradle + unit/integration test campaign.
9. Build debug APK and test on the physical Android device.
10. Fix all runtime, migration, navigation, camera, microphone, payment, offline and data-integrity failures before release.

## Portable next-session startup procedure

A new ChatGPT/Codex session should:

1. Inspect the active branch.
2. Read `README.md`.
3. Read `docs/ROADMAP.md`.
4. Read this `docs/DEVELOPER_HANDOFF_V24.md`.
5. Inspect recent commits and relevant source files.
6. Reconcile documentation with code.
7. Report a concise state audit before coding.
8. Continue with the highest-priority incomplete coherent slice.

## Definition of done

A feature is complete only when:

- UI exists
- business rules exist
- persistence exists where necessary
- navigation works
- loading/error/empty states are handled
- edge cases are covered
- adjacent workflows integrate
- appropriate unit tests exist
- offline behavior is considered
- documentation is updated
- final CI/device verification is completed later during the QA freeze
