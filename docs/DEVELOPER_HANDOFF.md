# RetailPOS V2 — Developer Handoff & Continuation Guide

> Current engineering handoff. The repository is the source of truth. Always inspect current code before changing it.

## Current repository state

- Repository: `ibrahim3596/retailappgpt`
- Active branch: `retailpos-v2`
- Current Room schema: **25**
- Current head after the latest hardening batch: `36715070954cc50597bee972c37c30722770f280`
- CI: **do not run GitHub Actions during the current Actions-budget restriction**.

The older handoff's Room version 11 and earlier commit references are historical and stale.

## Product direction

RetailPOS V2 is a shopkeeper-first, offline-first Indian retail POS. Core loops:

```text
SELL    Product → Cart → Payment → Stock ↓
BUY     Supplier → Purchase → Receive → Stock ↑ → Payable ↑
CREDIT  Customer → Credit/Khata → Payment → Balance ↓
```

Critical billing, pricing and inventory operations must remain deterministic/local.

## Non-regression business rules

### Loose goods / voice

Support decimal quantities and units such as kg, g, litre/l, ml, pcs, packet, pack, pouch, bottle, box, tin, jar and sachet.

Voice pipeline:

```text
speech → structured product/quantity/unit → local product resolution → validation → cart
```

Never silently confirm an ambiguous product. Hindi is baseline; Android recognizer capability must be detected for other Indian languages.

### GST

Supported store modes:

- `NO_GST`
- `REGULAR`
- `COMPOSITION`

Regular GST uses persisted product tax rates. Composition does not add GST as a separate customer charge. Historical sales preserve their original pricing/tax snapshot.

### Barcode / QR

Canonical barcode model: `product_barcodes`. `ProductEntity.barcode` is only a compatibility mirror.

Normal product scanning must reject arbitrary QR payloads. Payment/business QR remains separate.

### Intelligent product capture

Camera/OCR/catalog/visual recognition may suggest candidates, but weak evidence remains reviewable. Retailer-controlled selling price, purchase price, stock and SKU must not be silently overwritten.

## Current implementation foundations

Verified current foundations include:

- Product Master and metadata
- SKU and primary/alternate barcode handling
- GTIN/EAN/UPC validation foundation
- products without barcodes
- persistent catalog/identification cache
- OCR/candidate ranking/confidence/evidence foundations
- unknown barcode → identify/create → return-to-POS foundation
- multilingual voice and loose-item quantity foundations
- cart persistence and held bills
- bill discounts and item-discount authorization rules
- GST modes and product tax rates
- deterministic pricing/payment settlement
- checkout idempotency and recovery foundations
- FEFO batch allocation and expiry protection
- inventory receive/adjustment
- returns/refunds
- Customers/Khata credit sales
- supplier/purchase persistence
- staff permission foundations
- encrypted backup/restore
- Room migrations through version 25

Do not reimplement these blindly; inspect current source first.

## P0 hardening completed in the latest batch

### Checkout

Checkout is a Room transaction covering pricing, payment settlement, FEFO allocation, product stock deduction, inventory movements, sale persistence, cost allocations and CREDIT ledger insertion.

Latest hardening adds same-store ownership validation for CREDIT customers before the sale can be created. Existing protections include idempotency, archived-product rejection, transaction-time stock validation, batch decrement checks, expired-only batch rejection, payment settlement validation and discount authorization.

### Returns

Returns now re-load the original sale by `(storeId, saleId)` inside the transaction rather than trusting a stale caller object. CREDIT return customers are also checked against the same store. Quantity, refund, batch restoration, product stock restoration, inventory movement, return persistence and Khata adjustment remain atomic.

### Purchasing

Purchase recording now validates supplier ownership, purchase/line/store ownership, product ownership, duplicate products, quantity/rate/discount economics, purchase total reconciliation, batch ownership, batch quantities and expiry. Inventory receive movements reference the actual batch when batches are used. Supplier payable and payment ledger writes remain inside the same transaction.

`PurchaseRules.validateDraft()` was fixed so invalid lines are reported instead of causing a second-pass exception.

## Database / migrations

Room is version **25** with a sequential migration chain:

```text
1→2→3→4→5→6→7→8→9→10→11→12→13→14→15→16→17→18→19→20→21→22→23→24→25
```

The migration SQL was statically audited in this session. No destructive migration was found. However, an executable full migration-chain test suite is still missing.

Continue to treat schema changes as deliberate migrations. Do not use destructive fallback migration strategies.

## Store isolation / referential integrity

`storeId` is a hard business boundary. Current inspected paths enforce store ownership for products, metadata, barcode lookup, sales/idempotency, checkout products/customers, returns/sales/customers, and purchases/suppliers/products/lines/batches.

Remaining work is a repository-wide DAO audit for cross-store references and orphanable relationships, especially secondary workflows such as expenses, favorites, held bills, supplier history, reports and settings.

## Money precision

The project still uses `Double` extensively. Central pricing/settlement rules exist, but there is no complete fixed-scale money/quantity policy yet.

Do not rewrite the database wholesale. Next money batch should centralize:

- money rounding
- tax rounding
- line totals
- payment totals
- refunds
- reports

## Remaining P0/P1 work

1. Add executable Room integration tests for checkout, returns, purchases, Khata, recovery, idempotency and store isolation.
2. Add full migration-chain tests.
3. Complete DAO/repository store-isolation audit.
4. Audit return/refund analytics for double counting.
5. Complete money precision policy.
6. Complete price override enforcement/audit.
7. Finish item-discount integration without double application.
8. Complete inventory valuation, COGS and return-adjusted COGS.
9. Improve held-bill stale-stock UX.
10. Finish purchase/supplier and return UI workflows.
11. Continue intelligent capture hardening after transactional integrity is locked.

## Repository cleanup

`tmp/` contains historical verification/CI marker files. Enumerate the full directory before deletion and remove only genuine temporary artifacts. Do not use temporary files to trigger CI.

## CI discipline

GitHub Actions is not the debugging loop.

Do not trigger CI to discover compilation errors, rerun workflows repeatedly, create dummy commits, create verification PRs, or claim CI-green without an actual successful run for the exact commit.

Use repository inspection, static reasoning and unit tests first. Reserve deliberate CI verification for when the Actions budget is available.

## Development method

For every coherent batch:

1. Inspect actual implementation.
2. Identify root causes and dependencies.
3. Fix related defects together.
4. Add/update business-rule tests.
5. Inspect callers and adjacent workflows.
6. Check Room/schema implications.
7. Check offline/process-death behavior.
8. Update documentation.
9. Commit with a clear message.
10. Continue to the next dependent batch.

A feature is done only when UI, business rules, persistence, navigation, error/empty/loading states, edge cases, adjacent workflows, tests, offline behavior and documentation are covered. CI is a later deliberate gate.

## Next large batch

**Transaction/integration validation.** Prioritize executable tests and source audits for:

```text
checkout → payment → sale → stock → Khata
returns → refund → stock/batch → Khata → analytics
purchase → receive → batch → stock → supplier payable/payment
process death → recovery → idempotent retry
store A data → store B mutation rejection
migration 1→25
```

After that, proceed to money precision and inventory economics, then POS completion, staff/security hardening, intelligent capture hardening, UX polish, supplier/purchasing completion, reporting and final release validation.
