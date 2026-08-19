# RetailPOS V2 — Master Developer Roadmap

> **This is the master reference for development.** Before adding a feature, check this document. Before declaring a feature complete, update its status here.

RetailPOS V2 is being built as a **real shopkeeper-first retail POS**, not a demo. The target is a fast, reliable, offline-first system with intelligent product identification, strong product/barcode handling, billing, inventory, customers/Khata, reporting, staff controls, and eventual cloud sync.

## Current build state

**Active focus: Product Master → canonical SKU/barcode system → product list/search/filtering → product create/edit → intelligent identification.**

### Development rule

Build coherent vertical slices, validate logic locally, and reserve GitHub Actions for deliberate verification when the Actions budget is available. Do not use CI as the debugging loop.

---

# 1. Product architecture & engineering foundation

- [x] Android/Kotlin foundation
- [x] Jetpack Compose foundation
- [x] Room persistence foundation
- [x] CameraX foundation
- [x] ML Kit barcode/OCR foundation
- [ ] Finalize feature-oriented package structure
- [x] Establish `core / data / domain / feature / ui` boundaries (in progress; legacy files remain)
- [x] Keep business rules independent from Compose screens for identity/barcode rules
- [ ] Consistent ViewModel/UI-state pattern across all features
- [x] Repository boundaries
- [ ] Formal use-case layer across all major features
- [ ] Centralized design system
- [ ] Standard loading/error/empty/success states
- [ ] Standard navigation/back-stack behavior
- [x] Centralized product identity validation
- [ ] Logging/diagnostics strategy

## Data integrity

- [x] Stable IDs for product/barcode/cache foundations
- [x] Database migration strategy foundation
- [ ] Referential integrity across all entities
- [ ] Transaction boundaries for sales/inventory/Khata
- [x] Product + primary barcode save protected by Room transaction
- [ ] Money/quantity precision strategy
- [ ] Date/time/timezone strategy

---

# 2. Product master — current priority

## Product identity

- [x] Product entity foundation
- [x] Product DAO foundation
- [x] SKU foundation
- [x] Central SKU normalization/validation
- [x] Barcode entity foundation
- [x] Primary barcode
- [x] Multiple/alternate barcodes per product
- [x] Barcode type storage
- [x] GTIN/EAN/UPC identifier support
- [x] Barcode normalization/check validation foundation
- [x] Identifier uniqueness rules
- [x] Duplicate identifier detection
- [x] Product-without-barcode support
- [x] Canonical barcode lookup through `product_barcodes`
- [x] Product search includes alternate barcodes
- [x] Legacy single-barcode field treated as compatibility mirror rather than authoritative lookup source
- [ ] Retailer SKU auto-generation/manual SKU UX

## Product information

- [x] Product name
- [x] Brand
- [ ] Category/subcategory
- [ ] Unit of measure selector
- [ ] Pack size / quantity per pack
- [ ] Variant support
- [ ] Product image
- [ ] Description/notes
- [x] Purchase price/cost foundation
- [x] Selling price foundation
- [x] MRP foundation
- [ ] Tax configuration
- [ ] Discount configuration
- [x] Minimum stock threshold foundation
- [ ] Active/inactive state

## Product list / discovery

- [x] Product list screen foundation
- [x] Product search by name/brand/SKU/barcode
- [x] Canonical alternate-barcode search
- [x] Stock filter logic: All / Low stock / Out of stock
- [x] Stock filter unit tests
- [x] Product rows show selling price and stock state
- [ ] Product list filter UI polish
- [ ] Category filters
- [ ] Sort options
- [ ] Recently updated/recently sold views

## Product workflows

- [x] Add-product navigation foundation
- [x] Edit-product navigation foundation
- [ ] Complete create product workflow polish
- [ ] Complete edit product workflow polish
- [ ] Dedicated product detail view
- [ ] Dedicated barcode management section
- [ ] Duplicate/copy product
- [ ] Archive product
- [ ] Restore product
- [ ] Bulk product import
- [ ] Bulk product export
- [ ] Product backup/restore

## Built-in product/catalog intelligence

- [x] Barcode → public catalog lookup foundation
- [x] Catalog candidate matching foundation
- [x] Review-before-applying catalog result
- [x] Store-specific override principle for retailer-controlled fields
- [x] Persistent barcode-backed cache foundation
- [ ] Offline/common built-in product dataset
- [ ] Catalog source priority system
- [ ] Catalog data freshness/versioning

---

# 3. Intelligent Product Capture

## Capture inputs

- [x] Barcode scanning foundation
- [x] Camera foundation
- [x] OCR foundation
- [x] Visual/image-label foundation
- [x] Manual barcode entry fallback
- [x] Manual product search fallback
- [ ] Product image capture
- [ ] Multiple-frame capture

## Identification pipeline

- [x] Barcode/catalog/name/image evidence model foundation
- [x] Confidence classification
- [x] HIGH / GOOD / MEDIUM / LOW / NONE semantics
- [x] Review-before-save
- [x] Catalog candidate review actions
- [x] Confidence explanation
- [x] POS unknown-product identification flow foundation
- [x] Return-to-POS flow foundation
- [x] Persistent identification cache foundation
- [ ] Complete live-cache integration audit
- [ ] OCR-noise filtering integration
- [ ] Candidate ranking
- [ ] Pack-size extraction
- [ ] Reliable MRP/price extraction
- [ ] Category inference
- [ ] Unit inference
- [ ] Variant detection
- [ ] Dedicated image-assisted recognition service
- [ ] Human correction/feedback loop

## Safety

- [x] Weak guesses treated as suggestions
- [x] Evidence/confidence shown to retailer
- [x] Candidate can be rejected
- [x] Camera/OCR result can be kept instead of catalog result
- [x] Retailer price/stock fields preserved
- [x] Low-confidence result requires review

---

# 4. Barcode / QR system

- [x] Common retail barcode detection
- [x] Barcode normalization
- [x] GTIN check-digit validation foundation
- [x] Local canonical barcode lookup
- [x] Public catalog fallback
- [x] Duplicate barcode prevention
- [x] Multiple barcodes per product
- [x] Manual barcode entry
- [x] QR codes excluded from standard product scanner
- [x] Unsupported QR payload is not converted into a product
- [ ] Payment/business QR flow kept separate from product identification
- [x] Duplicate-scan debounce foundation
- [x] Scan callback
- [x] Camera permission flow
- [x] Flash/torch
- [ ] Camera retry/restart polish
- [ ] Low-light guidance

---

# 5. POS / billing

## Product entry

- [x] Cart foundation
- [x] Checkout rules foundation
- [x] Intelligent identification foundation
- [ ] Fast canonical product lookup in POS
- [ ] Barcode scan → cart
- [ ] Search → cart
- [ ] Unknown barcode → intelligent capture → create → return to bill complete integration
- [ ] Recently sold products
- [ ] Favorites/quick add

## Cart

- [ ] Add/remove item UI
- [ ] Quantity editing
- [ ] Decimal quantities
- [ ] Price override permissions
- [ ] Item discounts
- [ ] Bill discounts
- [ ] Tax calculations
- [ ] Totals
- [ ] Hold/resume bill
- [ ] Clear bill confirmation

## Checkout

- [ ] Cash
- [ ] Card
- [ ] UPI
- [ ] Other payment methods
- [ ] Split payment
- [ ] Amount tendered/change
- [ ] Payment validation
- [ ] Sale persistence
- [ ] Atomic inventory deduction
- [ ] Receipt generation
- [ ] Receipt preview/share/print
- [ ] Reprint

## Returns

- [ ] Find previous sale
- [ ] Full/partial return
- [ ] Return quantity validation
- [ ] Inventory restoration
- [ ] Refund recording
- [ ] Return reason
- [ ] Audit trail

---

# 6. Inventory

- [x] Inventory foundation
- [ ] Current stock views
- [ ] Stock-in
- [ ] Stock-out
- [ ] Adjustments
- [ ] Adjustment reasons
- [x] Stock movement foundation
- [ ] Inventory audit trail
- [ ] Suppliers
- [ ] Purchase entry
- [ ] Purchase history
- [x] Batch/lot foundation
- [x] Expiry foundation
- [ ] Low-stock alerts
- [ ] Out-of-stock state polish
- [ ] Near-expiry alerts
- [ ] FEFO/FIFO rules where applicable
- [ ] Inventory valuation
- [ ] Stock transfer
- [ ] Multi-location support

---

# 7. Customers & Khata

- [x] Customer data foundation
- [x] Khata data foundation
- [ ] Customer CRUD UI
- [ ] Customer search
- [ ] Purchase history
- [ ] Credit sale
- [ ] Credit limits
- [ ] Payment collection
- [ ] Partial payments
- [x] Ledger data foundation
- [ ] Ledger UI
- [ ] Outstanding balances
- [ ] Statements
- [ ] Payment receipts
- [ ] Credit audit trail

---

# 8. Suppliers & purchasing

- [ ] Supplier profiles
- [ ] Contacts
- [ ] Supplier/product associations
- [ ] Purchase workflows
- [ ] Purchase history
- [ ] Payables foundation
- [ ] Supplier search

---

# 9. Reports & business intelligence

- [ ] Today's sales
- [ ] Sales by range
- [ ] Bill count
- [ ] Average bill value
- [ ] Payment-method breakdown
- [ ] Discount/tax/returns summaries
- [ ] Best sellers
- [ ] Slow movers
- [ ] Category performance
- [ ] Product profit
- [ ] Current inventory
- [ ] Low/out-of-stock
- [ ] Stock movements
- [ ] Expiry
- [ ] Inventory valuation
- [ ] Khata outstanding
- [ ] Collections
- [ ] Expenses
- [ ] Gross sales / COGS / gross profit
- [ ] Dashboard
- [ ] Export

---

# 10. Expenses

- [ ] Expense categories
- [ ] Record expense
- [ ] Edit/delete with audit rules
- [ ] Expense history
- [ ] Expense reporting
- [ ] Profit integration

---

# 11. Staff, permissions & audit

- [ ] Staff profiles
- [ ] Authentication/access
- [ ] Roles
- [ ] Permission matrix
- [ ] Cashier restrictions
- [ ] Manager permissions
- [ ] Price override permission
- [ ] Discount permission
- [ ] Refund permission
- [ ] Inventory adjustment permission
- [ ] Reports permission
- [ ] Settings permission
- [ ] Staff activity audit

---

# 12. Store & business settings

- [ ] Store profile
- [ ] Address/contact
- [ ] Currency
- [ ] GST/tax configuration
- [ ] Invoice/receipt numbering
- [ ] Receipt branding
- [ ] Payment configuration
- [ ] Product/unit defaults
- [ ] Inventory defaults
- [ ] Notifications
- [ ] Staff settings

---

# 13. Offline-first reliability

- [x] Local DB foundation
- [x] Product lookup works locally for stored products
- [ ] Billing fully offline
- [ ] Inventory fully offline
- [ ] Customers/Khata fully offline
- [ ] Reports from local data
- [x] Intelligent capture has offline-safe fallback behavior
- [ ] Explicit online/offline state
- [ ] No silent data loss
- [ ] Retryable network operations
- [ ] Sync queue

---

# 14. Cloud, backup & sync

- [ ] Authentication
- [ ] Store identity
- [ ] Cloud backup
- [ ] Restore
- [ ] Sync engine
- [ ] Conflict detection
- [ ] Conflict resolution
- [ ] Sync status
- [ ] Failed-sync recovery
- [ ] Multi-device support
- [ ] Secure cloud data handling

---

# 15. Security & privacy

- [ ] Local data security
- [ ] Sensitive settings protection
- [ ] Permission enforcement in business logic
- [ ] Audit trail
- [ ] Secure export/backup
- [ ] Account/session handling
- [ ] Camera/OCR/image privacy review
- [ ] Network security review

---

# 16. UX / design system

- [ ] Typography tokens
- [ ] Spacing tokens
- [ ] Color tokens
- [ ] Shared buttons/fields/cards
- [ ] Shared dialogs/sheets
- [ ] Scanner UI consistency
- [ ] Product form consistency
- [ ] Empty/loading/error states
- [ ] Accessibility
- [ ] Touch-target audit
- [ ] Shopkeeper-speed audit
- [ ] Reduce billing taps

---

# 17. Repository / documentation quality

- [x] Professional README
- [x] Master roadmap
- [ ] Architecture documentation
- [ ] Product specification
- [ ] Data model
- [ ] Barcode/identifier specification
- [ ] Intelligent Capture specification
- [ ] Offline/sync specification
- [x] Initial unit-test structure
- [ ] Testing guide
- [ ] Contributing guide
- [ ] Changelog
- [ ] Remove stale temporary artifacts
- [ ] Naming conventions
- [ ] Commit conventions
- [ ] Remove dead/duplicate code

---

# 18. Testing

## Unit

- [x] Product identity rules
- [x] SKU normalization/validation
- [x] Barcode normalization/validation foundation
- [x] QR rejection rules
- [x] Identification confidence
- [x] Product list stock-filter rules
- [ ] Money calculations
- [ ] Quantity calculations
- [ ] Tax/discount calculations
- [ ] Checkout rules
- [ ] Inventory rules
- [ ] Khata balance
- [ ] Returns/refunds
- [ ] Candidate ranking

## Repository/data

- [ ] DAO CRUD
- [ ] Constraints
- [ ] Migrations
- [ ] Duplicate identifiers
- [x] Product + primary barcode transaction path
- [ ] Full transactional audit
- [x] Identification cache foundation
- [ ] Full offline behavior

## Integration

- [ ] Scan → local product
- [x] Unknown barcode → intelligent identification foundation
- [x] Identification → product save foundation
- [x] Identification → return to POS foundation
- [ ] POS → payment → sale → stock deduction
- [ ] Credit sale → Khata
- [ ] Payment → Khata settlement
- [ ] Return → stock restoration

## Device/manual

- [ ] Camera permission
- [ ] Barcode scanning
- [x] QR rejection behavior
- [ ] Low light
- [ ] Different packaging/barcode sizes
- [ ] Rapid scans
- [ ] Offline billing
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
- [ ] One deliberate CI verification cycle when Actions budget is available
- [ ] Production checklist
- [ ] Versioning/changelog

---

# 20. Definition of done

A feature is not done because a screen exists.

A feature is done when:

1. UI exists.
2. Business rules exist.
3. Persistence exists where required.
4. Navigation/back behavior works.
5. Loading/error/empty states are handled.
6. It integrates with related workflows.
7. Important edge cases are handled.
8. Appropriate tests exist.
9. Offline behavior is considered.
10. Documentation/roadmap is updated.
11. CI is deliberately verified later when the verification budget is available.

---

# 21. Priority order

1. **Product Master + canonical SKU/barcode system**
2. **Intelligent Product Capture**
3. **POS/billing**
4. **Inventory**
5. **Customers + Khata**
6. **Suppliers/purchasing**
7. **Reports + expenses**
8. **Staff/permissions + settings**
9. **Offline reliability**
10. **Cloud backup/sync**
11. **Testing, hardening and release**

Prefer completing an end-to-end workflow before jumping to unrelated UI.

---

# 22. CI budget rule

During periods when Actions minutes are scarce:

- Do not use CI to discover obvious compile errors.
- Do not trigger CI for tiny changes.
- Do not create dummy trigger commits.
- Do not repeatedly rerun unexplained failures.
- Validate locally/static/unit-level wherever possible.
- Batch coherent work.
- Reserve CI for deliberate integration verification and release hardening.
