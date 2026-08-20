# RetailPOS V2 — Master Developer Roadmap

> Master reference for development. Build coherent vertical slices, validate locally, and reserve GitHub Actions for deliberate verification when the budget is available.

RetailPOS V2 is a **shopkeeper-first, offline-first Indian retail POS**. The primary product loops are:

```text
SELL    Product → Cart → Payment → Stock ↓
BUY     Supplier → Purchase → Receive → Stock ↑ → Payable ↑
COLLECT Customer → Credit/Khata → Payment → Balance ↓
```

The owner view ultimately combines sales, cash/UPI/credit, inventory, receivables, payables and margin.

## Current build state

**Product Master is functionally complete as a foundation.** Intelligent Product Capture has a substantially complete local/offline evidence pipeline, review/confidence model, catalog fallback, feedback and exact unknown-product billing handoff. POS has transactional checkout, discounts, GST foundations, payment validation, receipts/reprints and persistent held-bill infrastructure. Customers/Khata is substantially wired end-to-end. Supplier/purchase persistence has now been added through Room 17→19 migrations, with atomic purchase-domain repository work in progress.

Advanced visual recognition, cloud sync, multi-store, forecasting and other enterprise features remain later priorities.

---

# 1. Product architecture & engineering foundation

- [x] Android/Kotlin foundation
- [x] Jetpack Compose foundation
- [x] Room persistence foundation
- [x] CameraX foundation
- [x] ML Kit barcode/OCR foundation
- [ ] Finalize feature-oriented package structure
- [x] Establish `core / data / domain / feature / ui` boundaries (legacy files remain)
- [x] Keep core business rules independent from Compose
- [ ] Consistent ViewModel/UI-state pattern across all features
- [x] Repository boundaries
- [ ] Formal use-case layer across all major features
- [ ] Centralized design system
- [ ] Standard loading/error/empty/success states
- [ ] Standard navigation/back-stack behavior
- [x] Centralized product identity validation
- [ ] Logging/diagnostics strategy

## Data integrity

- [x] Stable IDs for core entities
- [x] Database migration strategy foundation
- [ ] Referential integrity review across all entities
- [x] Transaction boundaries for core sales/inventory/Khata/purchase writes
- [x] Product + primary barcode save protected by Room transaction
- [x] Product + metadata saved in one atomic transaction
- [ ] Money/quantity precision strategy beyond Double
- [ ] Date/time/timezone strategy

---

# 2. Product Master

## Product identity

- [x] Product entity
- [x] Product DAO
- [x] SKU normalization/validation
- [x] Primary barcode
- [x] Alternate barcodes
- [x] Barcode type
- [x] GTIN/EAN/UPC support
- [x] Barcode normalization/check validation foundation
- [x] Identifier uniqueness rules
- [x] Duplicate identifier detection
- [x] Product-without-barcode support
- [x] Canonical lookup through `product_barcodes`
- [x] Alternate-barcode search
- [x] Legacy `ProductEntity.barcode` treated only as compatibility mirror
- [ ] Retailer SKU auto-generation/manual SKU UX

## Product information

- [x] Product name
- [x] Brand
- [x] Category/subcategory
- [x] Selling unit
- [x] Pack size / pack unit
- [ ] Variant persistence as first-class model
- [x] Product image persistence
- [x] Description/notes
- [x] Purchase/selling price foundation
- [x] MRP foundation
- [ ] Store-level GST configuration UI
- [x] Bill-level discount rules
- [x] Minimum stock threshold
- [ ] Active/archive/restore state

## Product discovery/workflows

- [x] Product list/search/filter
- [x] Product details
- [x] Create/edit product
- [x] Barcode management
- [ ] Category filters/sort polish
- [ ] Duplicate/copy product
- [ ] Archive/restore
- [ ] Bulk import/export
- [ ] Backup/restore

## Catalog intelligence

- [x] Public catalog lookup foundation
- [x] Candidate/review model
- [x] Store-specific override principle
- [x] Persistent barcode-backed cache
- [ ] Offline common-product dataset
- [ ] Catalog freshness/versioning

---

# 3. Intelligent Product Capture

## Capture

- [x] Barcode scanning
- [x] Camera
- [x] OCR
- [x] Visual evidence foundation
- [x] Manual barcode fallback
- [x] Manual search fallback
- [x] Image selection/persistence
- [x] Multi-frame capture

## Identification

- [x] Evidence model
- [x] Confidence classification
- [x] Review-before-save
- [x] Catalog candidate review
- [x] Confidence explanation
- [x] Unknown-product POS flow
- [x] Exact saved-product return-to-bill
- [x] Persistent identification cache
- [x] Offline cache-first catalog lookup
- [x] OCR cleanup
- [x] Candidate ranking
- [x] Pack extraction
- [x] MRP extraction foundation
- [x] Variant ranking
- [x] Local-vs-catalog conflict resolver
- [x] Exact local barcode evidence before catalog fallback
- [x] Bounded persistent correction feedback
- [ ] Authoritative category inference
- [ ] Automatic selling-unit inference
- [ ] Dedicated image-recognition service
- [ ] Offline common-product vision dataset
- [ ] Device/release capture hardening

## Safety

- [x] Weak guesses stay suggestions
- [x] Evidence/confidence shown
- [x] Candidate rejection
- [x] Camera/OCR data can be retained
- [x] Retailer price/stock/SKU preserved
- [x] Low-confidence requires review
- [x] Pack quantity never overwrites selling unit
- [x] Conflicting evidence never silently confirms identity

---

# 4. Barcode / QR

- [x] Common retail barcodes
- [x] Normalization
- [x] GTIN validation foundation
- [x] Canonical local lookup
- [x] Public catalog fallback
- [x] Duplicate prevention
- [x] Multiple barcodes
- [x] Manual barcode entry
- [x] QR rejection for normal product identification
- [x] Unsupported QR not converted into a product
- [ ] Separate payment/business QR feature
- [x] Scan debounce foundation
- [x] Permission/torch
- [ ] Retry/restart polish
- [ ] Low-light guidance

---

# 5. POS / Billing

## Product entry

- [x] Cart foundation
- [x] Search → cart
- [x] Barcode scan → cart
- [x] Intelligent unknown-product → create → return to bill
- [ ] Recently sold UI integration
- [ ] Favorites persistence/UI integration
- [ ] Fast canonical lookup polish
- [x] Voice billing foundation

## Cart

- [x] Add/remove item UI
- [x] Quantity editing
- [x] Decimal quantities
- [x] Bill discount
- [x] Tax calculation foundation
- [x] Totals
- [x] Clear bill confirmation
- [x] Hold/resume infrastructure, now Room-persistent
- [ ] Item-level discounts
- [ ] Price override flow

## Checkout

- [x] Cash
- [x] Card amount validation
- [x] UPI amount validation
- [x] Credit/Khata
- [ ] Split payment
- [x] Amount tendered/change persistence
- [x] Payment validation
- [x] Sale persistence
- [x] Atomic inventory deduction
- [x] Receipt generation
- [x] Receipt share/reprint foundation
- [ ] Thermal printer integration

## Returns

- [ ] Find previous sale
- [ ] Full/partial return
- [ ] Return quantity validation
- [ ] Inventory restoration
- [ ] Refund recording
- [ ] Return reason
- [ ] Refund audit trail

---

# 6. Inventory

- [x] Inventory foundation
- [x] Current stock views
- [x] Stock receiving foundation
- [x] Stock adjustment foundation
- [x] Adjustment reasons
- [x] Stock movements
- [x] Negative-stock protection
- [x] Batch/lot foundation
- [x] Expiry validation
- [x] FEFO-safe batch lookup
- [x] Low/out-of-stock status rules
- [ ] Near-expiry alert UI
- [ ] Inventory valuation
- [ ] COGS integration
- [ ] Stock transfer
- [ ] Multi-location

---

# 7. Customers & Khata

- [x] Customer data foundation
- [x] Customer CRUD/search UI
- [x] Customer → Khata navigation
- [x] Credit sale
- [x] Ledger
- [x] Live outstanding balance
- [x] Partial payments
- [x] Full settlement
- [x] Overpayment rejection
- [x] Statement/share foundation
- [x] Delete protection while balance is outstanding
- [ ] Credit limits
- [ ] Payment receipt artifact
- [ ] Dedicated purchase history view
- [ ] Credit audit/reporting polish

---

# 8. Suppliers & Purchasing

- [x] Supplier entity foundation
- [x] Supplier DAO foundation
- [x] Supplier ledger entity
- [x] Purchase entity foundation
- [x] Purchase line foundation
- [x] Free quantity / scheme economics
- [x] Supplier payable rules
- [x] Room 18→19 persistence migration
- [x] Atomic purchase repository foundation
- [ ] Supplier management UI
- [ ] Supplier search UI
- [ ] Purchase entry UI
- [ ] Purchase history UI
- [ ] Supplier payment UI
- [ ] Supplier/product associations
- [ ] Purchase return/debit note workflow

---

# 9. Reports & Owner Dashboard

- [x] Payment-method reconciliation rules
- [ ] Today's sales screen
- [ ] Sales by date range
- [ ] Bill count
- [ ] Average bill value
- [ ] Cash/UPI/card/credit dashboard
- [ ] Discount/tax/returns summaries
- [ ] Best sellers
- [ ] Slow movers
- [ ] Category performance
- [ ] Product gross profit
- [ ] Current inventory summary
- [ ] Expiry summary
- [ ] Inventory valuation
- [ ] Khata outstanding
- [ ] Supplier payables
- [ ] Purchase summary
- [ ] Collections
- [ ] Expenses
- [ ] Gross sales / COGS / gross profit
- [ ] Dashboard
- [ ] Export

---

# 10. Expenses

- [ ] Categories
- [ ] Record expense
- [ ] Edit/delete with audit rules
- [ ] Expense history
- [ ] Expense reporting
- [ ] Profit integration

---

# 11. Staff, Permissions & Audit

- [x] Local staff accounts
- [x] PIN authentication
- [x] Owner/Manager/Cashier roles
- [x] Discount permission enforcement in checkout
- [x] Staff activation/deactivation foundation
- [x] PIN reset foundation
- [x] Cashier switching/session gate foundation
- [ ] Price override permission workflow
- [ ] Item discount permission workflow
- [ ] Refund permission
- [ ] Inventory-adjustment permission
- [ ] Report permission UI
- [ ] Settings permission UI
- [ ] Staff activity audit
- [ ] Session timeout/lock

---

# 12. Store & Business Settings

- [ ] Store profile
- [ ] Address/contact
- [x] Currency foundation
- [ ] GST mode configuration UI
- [ ] Invoice/receipt numbering
- [ ] Receipt branding
- [ ] Payment configuration
- [ ] Product/unit defaults
- [ ] Inventory defaults
- [ ] Notifications
- [ ] Staff management navigation polish

---

# 13. Offline-first reliability

- [x] Local DB foundation
- [x] Stored-product lookup offline
- [x] Intelligent capture offline cache fallback
- [x] Held bills persist locally
- [x] Customers/Khata local data
- [x] Supplier/purchase domain is local-first
- [ ] Explicit online/offline state
- [ ] Full billing recovery audit
- [ ] Full inventory recovery audit
- [ ] Retryable network operations
- [ ] Sync queue
- [ ] No-silent-data-loss audit

---

# 14. Cloud, backup & sync

- [ ] Authentication
- [ ] Store identity
- [ ] Backup
- [ ] Restore
- [ ] Sync engine
- [ ] Conflict detection/resolution
- [ ] Sync status
- [ ] Failed-sync recovery
- [ ] Multi-device
- [ ] Secure cloud handling

---

# 15. Security & privacy

- [ ] Local database security review
- [x] PIN hash storage
- [x] Business-rule permission enforcement for discounts
- [ ] Full permission matrix enforcement
- [ ] Audit trail
- [ ] Secure export/backup
- [ ] Camera/OCR/image privacy review
- [ ] Network security review

---

# 16. UX / Shopkeeper speed

- [ ] Typography/spacing/color tokens
- [ ] Shared buttons/fields/cards/dialogs
- [ ] Shared scanner UI
- [ ] Standard loading/error/empty states
- [ ] Accessibility
- [ ] Touch-target audit
- [ ] Shopkeeper-speed audit
- [ ] Reduce billing taps
- [ ] Favorites/recently-sold quick add UI
- [ ] Voice-language download/settings UI

---

# 17. Documentation

- [x] README
- [x] Master roadmap
- [x] Intelligent Capture docs
- [x] Buy-loop specification
- [ ] Architecture document
- [ ] Product specification
- [ ] Data model
- [ ] Barcode specification
- [ ] Offline/sync specification
- [ ] Testing guide
- [ ] Contributing guide
- [ ] Changelog
- [ ] Remove stale temporary artifacts
- [ ] Naming/commit conventions
- [ ] Dead/duplicate code audit

---

# 18. Testing

## Unit

- [x] Product identity
- [x] SKU/barcode rules
- [x] QR rejection
- [x] Capture confidence/ranking
- [x] Pack extraction
- [x] Candidate conflict resolver
- [x] Identification feedback
- [x] Purchase economics
- [x] Supplier payable rules
- [x] Inventory validation/status rules
- [x] Khata payment rules
- [x] Day-end reconciliation rules
- [ ] Money precision strategy
- [ ] Full checkout rules
- [ ] Returns/refunds

## Repository/data

- [ ] Full DAO CRUD suite
- [ ] Migration 17→18 verification
- [ ] Migration 18→19 verification
- [ ] Full migration chain verification
- [x] Product + barcode transaction
- [x] Product + metadata transaction
- [x] Atomic purchase transaction foundation
- [ ] Full transaction audit

## Integration

- [ ] Scan → local product
- [x] Unknown barcode → identify → save → bill
- [ ] POS → payment → sale → stock full integration test
- [ ] Credit sale → Khata integration test
- [ ] Payment → Khata settlement integration test
- [ ] Purchase → stock/batch/payable integration test
- [ ] Return → stock/refund integration test

## Device/manual

- [ ] Camera permission
- [ ] Barcode scanning
- [x] QR rejection
- [ ] Low light
- [ ] Different packaging/barcode sizes
- [ ] Rapid scans
- [ ] Offline billing
- [ ] Persistent held bill after process restart
- [ ] Printer/receipt workflow

---

# 19. Release hardening

- [ ] Full unit test pass
- [ ] Repository/integration tests
- [ ] Critical UI flow pass
- [ ] Physical device pass
- [ ] Performance pass
- [ ] Memory/battery pass
- [ ] Security/privacy review
- [ ] Migration review
- [ ] Backup/restore test
- [ ] Offline/recovery test
- [ ] Release build
- [ ] Deliberate CI verification when Actions budget is available
- [ ] Production checklist
- [ ] Versioning/changelog

---

# 20. Current priority order

1. **POS/billing completion** — receipts, split/real payment UX, recent/favorites, item discounts/price overrides
2. **Suppliers/purchasing UI** — supplier CRUD, purchase entry, receiving, payable settlement
3. **Inventory economics** — purchase cost, COGS, valuation, expiry dashboard
4. **Returns/refunds**
5. **Owner dashboard + day-end reconciliation UI**
6. **Staff/permissions hardening**
7. **Advanced Intelligent Capture** — image recognition/dataset/device hardening
8. **Offline reliability hardening**
9. **Cloud backup/sync**
10. **Final testing/release**

The product is intentionally **not** prioritizing enterprise ERP complexity or advanced AI forecasting before the core `SELL / BUY / COLLECT` loops are complete.

---

# 21. CI budget rule

During periods when Actions minutes are scarce:

- Do not use CI to discover obvious compilation errors.
- Do not trigger CI for tiny changes.
- Do not create dummy trigger commits.
- Do not repeatedly rerun failures.
- Use source inspection, local/static reasoning and unit tests.
- Batch coherent changes.
- Reserve CI for deliberate verification and release hardening.
