# RetailPOS V2 — Master Developer Roadmap

> Current master roadmap. The repository is the source of truth. Build coherent vertical slices, validate with source inspection/unit tests, and reserve GitHub Actions for deliberate verification when the Actions budget is available.

## Current state

- Branch: `retailpos-v2`
- Room schema: **25**
- Core Product Master: mature foundation
- Intelligent Product Capture: strong local/offline evidence, review and candidate pipeline; advanced device/vision work remains
- POS: transactional checkout, pricing, GST, payment validation, receipts and held bills are implemented foundations
- Customers/Khata: credit sales, ledger and payment foundations are implemented
- Suppliers/Purchasing: persistence and atomic repository foundation are implemented
- Returns: transactional repository foundation is implemented
- Inventory: stock, movements, batches, expiry and FEFO foundations are implemented
- Staff: authentication/roles/permission foundations exist
- Backup/restore: encrypted local backup foundations exist

## Priority rule

Do not spend the next cycles polishing screens while transactional integrity is untested. Core business mutations must be correct before broad UI redesign.

## P0 — Transactional integrity

### Checkout

- [x] Cart → pricing → discount → GST → payment → sale → inventory → Khata transaction foundation
- [x] Checkout idempotency
- [x] Process-death/recovery foundations
- [x] Transaction-time stock validation
- [x] FEFO batch allocation
- [x] Expired-only batch rejection
- [x] CREDIT customer store ownership validation
- [ ] Executable end-to-end checkout integration tests
- [ ] Duplicate checkout/retry integration tests
- [ ] Rollback/process-death integration tests
- [ ] Stale-stock integration tests
- [ ] Credit-total reconciliation test

### Returns

- [x] Persisted original-sale lookup inside transaction
- [x] Full/partial quantity validation foundation
- [x] Already-returned quantity protection
- [x] Batch restoration foundation
- [x] Product stock restoration
- [x] Refund persistence
- [x] CREDIT Khata adjustment
- [x] Same-store sale/customer checks
- [ ] Previous-sale lookup UI
- [ ] Full/partial return UI completion
- [ ] Refund receipt/audit integration
- [ ] Return-adjusted analytics/COGS integration tests

### Purchasing

- [x] Supplier ownership validation
- [x] Purchase/line/store ownership validation
- [x] Product ownership validation
- [x] Purchase economics reconciliation
- [x] Batch ownership/quantity/expiry validation
- [x] Atomic stock + batch + supplier-ledger mutation
- [x] Supplier payable/payment writes in same transaction
- [ ] Supplier CRUD UI
- [ ] Purchase entry UI
- [ ] Purchase history UI
- [ ] Supplier payment UI
- [ ] Purchase return/debit-note workflow
- [ ] End-to-end purchase integration tests

## P0 — Database integrity

- [x] Room schema version 25
- [x] Sequential migrations 1→25
- [x] Static audit of migration SQL
- [ ] Executable migration tests for every migration boundary
- [ ] Full 1→25 migration test
- [ ] DAO/repository referential integrity audit
- [ ] Cross-store isolation tests
- [ ] Orphan-reference audit

## P1 — Money and pricing

- [x] Central pricing rules foundation
- [x] Central payment settlement rules
- [x] Tax-mode rules
- [ ] Central money rounding policy
- [ ] Central tax rounding policy
- [ ] Line-total rounding policy
- [ ] Refund rounding policy
- [ ] Report rounding policy
- [ ] Price override permission enforcement
- [ ] Explicit price-override audit reason
- [ ] Original-price retention in sale snapshot
- [ ] Item discounts without double application
- [ ] Split payments using the same settlement engine

Do not rewrite the entire database from `Double` immediately. Centralize calculations first, then migrate persistence deliberately if required.

## P1 — Inventory economics

- [x] Stock movements
- [x] Batch/lot model
- [x] Expiry rules
- [x] FEFO selection
- [ ] Expiry UI
- [ ] Inventory valuation
- [ ] COGS
- [ ] Return-adjusted COGS
- [ ] Stock recovery/reconciliation
- [ ] Stock transfer
- [ ] Multi-location later

## P1 — Held bills

- [x] Room-persistent held bills
- [x] Atomic claim path
- [ ] Resume-time stale-stock validation UX
- [ ] Clearly identify affected lines
- [ ] Allow correction/removal only for affected lines before checkout

## P1 — POS completion

- [x] Search → cart
- [x] Barcode → cart
- [x] Voice billing foundation
- [x] Decimal/loose-item quantities
- [x] Favorites/recent infrastructure foundations
- [ ] Faster quick-add UX
- [ ] Recently-sold UI integration
- [ ] Favorites UI integration
- [ ] Scanner duplication cleanup
- [ ] Voice review/retry polish
- [ ] Item discount UI
- [ ] Price override UI
- [ ] Split payment UI
- [ ] Thermal printer integration later

## P1 — Staff/security

Business authorization must be enforced at mutation boundaries, not only by hiding UI controls.

- [x] Staff roles
- [x] PIN authentication
- [x] Core discount permission enforcement
- [ ] Price override permission
- [ ] Refund permission
- [ ] Inventory adjustment permission
- [ ] Report permission
- [ ] Settings permission
- [ ] Staff activity audit
- [ ] Session timeout/lock
- [ ] Route vs operation-level authorization audit

## P1 — Intelligent Product Capture

After transactional integrity is locked:

- [x] Barcode/OCR/visual evidence foundation
- [x] Candidate ranking
- [x] Confidence/evidence review
- [x] Persistent identification cache
- [x] Local-first lookup
- [x] Catalog fallback
- [x] Correction feedback foundation
- [ ] Authoritative category inference
- [ ] Authoritative unit inference
- [ ] Catalog freshness strategy
- [ ] Device capture hardening
- [ ] Low-light/retry UX
- [ ] Rapid-scan debounce hardening
- [ ] Dedicated image-recognition service
- [ ] Offline common-product dataset

Never let weak vision guesses silently become confirmed products.

## P2 — Owner/business workflows

- [ ] Sales dashboard
- [ ] Cash/UPI/card/credit breakdown
- [ ] Gross sales
- [ ] COGS
- [ ] Gross profit
- [ ] Returns/discount/tax reporting
- [ ] Inventory valuation
- [ ] Expiry dashboard
- [ ] Receivables
- [ ] Supplier payables
- [ ] Purchase summary
- [ ] Expense integration
- [ ] Day-end reconciliation UI
- [ ] Export/reporting

## P2 — Repository cleanup

`tmp/` contains historical CI/verification marker artifacts.

- [ ] Enumerate the entire directory
- [ ] Identify genuine temporary artifacts
- [ ] Remove only temporary artifacts
- [ ] Preserve legitimate product assets
- [ ] Audit stale/duplicate documentation
- [ ] Remove abandoned implementations only after caller verification

## P2 — Offline reliability

- [x] Local Room database
- [x] Local product lookup
- [x] Local cart/held bills
- [x] Local Khata
- [x] Local purchasing foundation
- [ ] Full offline billing recovery test
- [ ] Inventory recovery test
- [ ] Retryable network operations
- [ ] Sync queue
- [ ] No-silent-data-loss audit

## P3 — Backup/cloud

- [x] Encrypted local backup/restore foundations
- [ ] Backup UX hardening
- [ ] Cloud backup
- [ ] Multi-device sync
- [ ] Conflict resolution
- [ ] Sync status/recovery
- [ ] Multi-store platform

## Testing priorities

Business-rule tests are more important than cosmetic UI tests.

Required coverage:

- [x] Pricing rules
- [x] Payment settlement
- [x] Inventory rules
- [x] Khata rules
- [x] Purchase economics
- [x] Return rules
- [x] Voice parsing foundations
- [x] Intelligent capture foundations
- [ ] Checkout integration
- [ ] Return integration
- [ ] Purchase integration
- [ ] Store isolation
- [ ] Idempotency
- [ ] Process recovery
- [ ] Migration 1→25
- [ ] Backup/restore recovery

## Definition of done

A feature is complete only when:

1. UI exists where required.
2. Business rules exist.
3. Persistence is correct.
4. Navigation works.
5. Loading/error/empty states work.
6. Edge cases are handled.
7. Adjacent workflows work.
8. Tests exist.
9. Offline/process-death behavior is considered.
10. Documentation is updated.
11. CI is deliberately verified later when the budget allows.

## CI rule

Do not trigger CI to discover compilation errors. Do not rerun workflows repeatedly, create dummy commits, or create verification PRs. Never claim CI-green without an actual successful run for the exact commit.
