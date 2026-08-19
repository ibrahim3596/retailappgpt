# RetailPOS V2 — Master Developer Roadmap

> **This is the master reference for development.** Before adding a feature, check this document. Before declaring a feature complete, update its status here.

RetailPOS V2 is being built as a **real shopkeeper-first retail POS**, not a demo. The target is a fast, reliable, offline-first system with intelligent product identification, strong product/barcode handling, billing, inventory, customers/Khata, reporting, staff controls, and eventual cloud sync.

---

# 0. Non-negotiable development rules

- [x] Build in coherent feature slices, not random isolated screens.
- [x] Keep core business logic separate from UI.
- [x] Prefer local/offline operation for core retail workflows.
- [x] Treat GitHub Actions as a verification gate, not as the debugging environment.
- [ ] Perform local/static/unit validation before consuming CI.
- [ ] Batch related changes before CI verification.
- [ ] Avoid throwaway CI-trigger commits and temporary verification files.
- [ ] Keep the repository understandable and production-quality.
- [ ] Update this roadmap whenever architecture or product scope is materially changed.

---

# 1. Product architecture & engineering foundation

## Application architecture

- [x] Android/Kotlin foundation
- [x] Jetpack Compose foundation
- [x] Room persistence foundation
- [x] CameraX foundation
- [x] ML Kit barcode/OCR foundation
- [ ] Finalize feature-oriented package structure
- [ ] Establish `core / data / domain / feature / ui` boundaries
- [ ] Keep business rules independent of Compose screens
- [ ] Establish consistent ViewModel/UI-state pattern
- [ ] Establish repository/use-case boundaries
- [ ] Centralize shared design system
- [ ] Standardize loading/error/empty/success states
- [ ] Standardize navigation and back-stack behavior
- [ ] Standardize validation and user-facing error messages
- [ ] Establish logging/diagnostic strategy

## Data integrity

- [ ] Stable IDs for all core entities
- [ ] Database migration strategy
- [ ] Referential integrity rules
- [ ] Transaction boundaries for sales/inventory/Khata
- [ ] Audit-sensitive operations protected from partial writes
- [ ] Money/quantity precision strategy
- [ ] Date/time and timezone strategy

---

# 2. Product master — complete product system

## Product identity

- [x] Product entity foundation
- [x] Product DAO foundation
- [x] SKU foundation
- [x] Barcode entity foundation
- [ ] Retailer SKU generation/manual SKU
- [ ] Primary barcode
- [ ] Multiple/alternate barcodes per product
- [ ] Barcode type storage
- [ ] GTIN/EAN/UPC identifier support
- [ ] Identifier normalization
- [ ] Identifier uniqueness rules
- [ ] Duplicate identifier detection
- [ ] Product-without-barcode support

## Product information

- [ ] Product name
- [ ] Brand
- [ ] Category/subcategory
- [ ] Unit of measure
- [ ] Pack size / quantity per pack
- [ ] Variant support
- [ ] Product image
- [ ] Description/notes
- [ ] Purchase price/cost
- [ ] Selling price
- [ ] MRP where applicable
- [ ] Tax configuration
- [ ] Discount configuration
- [ ] Minimum stock level
- [ ] Active/inactive product state

## Product workflows

- [ ] Create product
- [ ] Edit product
- [ ] Duplicate/copy product
- [ ] Archive product
- [ ] Restore product
- [ ] Product search
- [ ] Filter/sort products
- [ ] Category browsing
- [ ] Product detail page
- [ ] Bulk product import
- [ ] Bulk product export
- [ ] Product backup/restore

## Built-in product/catalog intelligence

- [ ] Built-in/common product catalog support
- [ ] Barcode → product lookup
- [ ] Catalog candidate matching
- [ ] Catalog result review before saving
- [ ] Store-specific overrides for catalog data
- [ ] Never overwrite retailer-controlled price/stock without explicit action
- [ ] Local cache of successfully resolved product identities

---

# 3. Intelligent Product Capture — flagship feature

> **Goal:** The retailer should be able to point the phone at a product and have the app intelligently determine what it is, not merely read a barcode.

## Capture inputs

- [x] Barcode scanning foundation
- [x] Camera foundation
- [x] OCR foundation
- [x] Visual/image-label foundation
- [ ] Product image capture
- [ ] Multiple-frame capture where useful
- [ ] Manual barcode entry fallback
- [ ] Manual product search fallback

## Identification pipeline

```text
Camera / barcode / OCR / visual evidence
                    ↓
             Normalize signals
                    ↓
             Candidate generation
                    ↓
       Barcode/catalog/name/image matching
                    ↓
           Evidence aggregation
                    ↓
          Confidence classification
                    ↓
             Human review
                    ↓
          Product creation/update
```

- [x] Catalog lookup foundation
- [x] Identification evidence model
- [x] Confidence levels: HIGH / GOOD / MEDIUM / LOW / NONE
- [x] Review-before-save principle
- [ ] Complete persistent identification integration
- [ ] Robust OCR-noise filtering integration
- [ ] Better candidate ranking
- [ ] Product-name/brand extraction
- [ ] Pack-size extraction
- [ ] MRP/price extraction where reliable
- [ ] Category inference
- [ ] Unit inference
- [ ] Variant detection
- [ ] Image-assisted product recognition service
- [ ] Human correction/feedback loop

## Intelligent safety rules

- [ ] Never silently turn a weak visual guess into a confirmed product
- [ ] Show why a candidate was selected
- [ ] Show confidence/evidence to the retailer
- [ ] Allow retailer to reject the suggestion
- [ ] Allow retailer to keep camera/OCR data instead of catalog data
- [ ] Preserve retailer-controlled fields
- [ ] Require review when confidence is insufficient

## Identification persistence

- [x] Local identification cache foundation
- [ ] Persist successful barcode/product identity mappings in live flow
- [ ] Store-scoped identity cache
- [ ] Reuse cached identity on subsequent scans
- [ ] Cache invalidation/versioning
- [ ] Allow retailer correction to override a bad match

---

# 4. Barcode / QR scanning system

> Barcode scanning must be retail-aware. Not every code visible on packaging is a product identifier.

## Supported identification behavior

- [ ] Detect common retail barcodes
- [ ] Normalize scanned values
- [ ] Validate barcode format/check digit where applicable
- [ ] Resolve barcode against local products first
- [ ] Resolve unknown barcode through catalog when available
- [ ] Offer intelligent identification for unknown products
- [ ] Prevent duplicate barcode assignment
- [ ] Support multiple barcodes on one product
- [ ] Manual barcode entry fallback

## QR handling

- [ ] Distinguish product barcodes from QR codes
- [ ] Ignore/reject unsupported QR codes during normal product scanning
- [ ] Do not create a product from an arbitrary QR payload
- [ ] Explain why an unsupported QR was rejected when appropriate
- [ ] Keep payment/business QR handling separate from product identification

## Scanner UX

- [ ] Fast repeated scanning
- [ ] Duplicate-scan debounce
- [ ] Scan success feedback
- [ ] Unknown-code state
- [ ] Invalid-code state
- [ ] Unsupported-code state
- [ ] Camera permission handling
- [ ] Flash/torch control
- [ ] Camera retry/restart handling
- [ ] Low-light guidance

---

# 5. POS / billing — core daily workflow

## Product entry

- [ ] Barcode scan → cart
- [ ] Product search → cart
- [ ] Intelligent identification from unknown barcode
- [ ] Intelligent product capture directly from POS
- [ ] Newly identified product → save → return to active bill
- [ ] Recently sold products
- [ ] Quick-add/favorites where appropriate

## Cart

- [x] Cart model foundation
- [ ] Add/remove item
- [ ] Quantity increase/decrease
- [ ] Decimal quantity for applicable units
- [ ] Price override permissions
- [ ] Item-level discount
- [ ] Bill-level discount
- [ ] Tax calculation
- [ ] Cart subtotal
- [ ] Tax total
- [ ] Discount total
- [ ] Grand total
- [ ] Hold/suspend bill
- [ ] Resume held bill
- [ ] Clear bill with confirmation

## Checkout

- [x] Checkout rules foundation
- [ ] Cash payment
- [ ] Card payment
- [ ] UPI/payment method support
- [ ] Other/custom payment methods
- [ ] Split payment where required
- [ ] Amount tendered
- [ ] Change calculation
- [ ] Payment validation
- [ ] Sale transaction persistence
- [ ] Stock deduction atomically with completed sale
- [ ] Receipt generation
- [ ] Receipt preview
- [ ] Print/share receipt
- [ ] Reprint previous receipt

## Returns/refunds

- [ ] Find previous sale
- [ ] Full return
- [ ] Partial return
- [ ] Return quantity validation
- [ ] Inventory restoration
- [ ] Refund recording
- [ ] Return reason
- [ ] Return audit trail

---

# 6. Inventory management

## Stock

- [x] Inventory data foundation
- [ ] Current stock quantity
- [ ] Stock by product
- [ ] Stock-in
- [ ] Stock-out
- [ ] Manual adjustment
- [ ] Adjustment reason
- [ ] Stock movement history
- [ ] Inventory audit trail

## Purchasing

- [ ] Supplier entity
- [ ] Supplier management
- [ ] Purchase entry
- [ ] Purchase invoice/reference
- [ ] Purchase cost
- [ ] Purchase quantity
- [ ] Purchase stock-in
- [ ] Purchase history

## Advanced inventory

- [ ] Low-stock threshold
- [ ] Low-stock alerts
- [ ] Out-of-stock state
- [ ] Batch/lot tracking
- [ ] Expiry date tracking
- [ ] Near-expiry alerts
- [ ] FEFO/FIFO strategy where applicable
- [ ] Inventory valuation
- [ ] Stock transfer
- [ ] Multi-location foundation if required later

---

# 7. Customers & Khata

## Customers

- [x] Customer data foundation
- [ ] Customer creation/editing
- [ ] Phone/contact information
- [ ] Customer search
- [ ] Customer purchase history
- [ ] Customer notes

## Khata / credit

- [x] Khata data foundation
- [ ] Credit sale
- [ ] Customer credit limit
- [ ] Payment collection
- [ ] Partial payment
- [ ] Ledger entries
- [ ] Outstanding balance
- [ ] Balance history
- [ ] Customer statement
- [ ] Payment receipt
- [ ] Credit/Khata transaction audit

---

# 8. Suppliers

- [ ] Supplier profiles
- [ ] Supplier contact details
- [ ] Supplier product associations
- [ ] Purchase history
- [ ] Supplier outstanding/payables foundation
- [ ] Supplier search

---

# 9. Reports & business intelligence

## Sales

- [ ] Today's sales
- [ ] Sales by date range
- [ ] Number of bills
- [ ] Average bill value
- [ ] Payment-method breakdown
- [ ] Discount summary
- [ ] Tax summary
- [ ] Returns summary

## Products

- [ ] Best-selling products
- [ ] Slow-moving products
- [ ] Product sales history
- [ ] Category performance
- [ ] Profit by product

## Inventory

- [ ] Current stock report
- [ ] Low-stock report
- [ ] Out-of-stock report
- [ ] Stock movement report
- [ ] Expiry report
- [ ] Inventory valuation

## Customers / Khata

- [ ] Outstanding credit
- [ ] Customer balances
- [ ] Collections report
- [ ] Customer purchase history

## Business

- [ ] Gross sales
- [ ] Cost of goods
- [ ] Gross profit
- [ ] Expenses
- [ ] Net business view
- [ ] Dashboard summaries
- [ ] Export reports

---

# 10. Expenses

- [ ] Expense categories
- [ ] Record expense
- [ ] Edit/delete with audit rules
- [ ] Expense history
- [ ] Expense reporting
- [ ] Profit calculation integration

---

# 11. Staff, roles & permissions

- [ ] Staff profiles
- [ ] Staff login/access
- [ ] Roles
- [ ] Permission matrix
- [ ] Cashier restrictions
- [ ] Manager permissions
- [ ] Price override permission
- [ ] Discount permission
- [ ] Refund permission
- [ ] Inventory adjustment permission
- [ ] Reports permission
- [ ] Settings/admin permission
- [ ] Staff activity/audit trail

---

# 12. Store & business settings

- [ ] Store/business profile
- [ ] Store name/address/contact
- [ ] Currency
- [ ] Tax/GST configuration
- [ ] Invoice/receipt numbering
- [ ] Receipt header/footer
- [ ] Receipt logo
- [ ] Payment configuration
- [ ] Product/unit defaults
- [ ] Inventory defaults
- [ ] Notification settings
- [ ] Staff settings

---

# 13. Offline-first reliability

> Core selling operations must remain useful without a live network.

- [x] Local database foundation
- [ ] Audit all critical workflows for offline operation
- [ ] Billing works offline
- [ ] Product lookup works offline
- [ ] Inventory works offline
- [ ] Customer/Khata works offline
- [ ] Reports work from local data
- [ ] Intelligent capture has graceful offline fallback
- [ ] Clear online/offline state
- [ ] No silent data loss
- [ ] Retryable network operations
- [ ] Sync queue foundation

---

# 14. Cloud, backup & synchronization

> Cloud services come after the local foundation is stable.

- [ ] Authentication
- [ ] Business/store identity
- [ ] Cloud backup
- [ ] Data restore
- [ ] Sync engine
- [ ] Sync queue
- [ ] Conflict detection
- [ ] Conflict resolution strategy
- [ ] Sync status UI
- [ ] Failed-sync recovery
- [ ] Multi-device support
- [ ] Secure data handling

---

# 15. Security, privacy & auditability

- [ ] Secure local data handling
- [ ] Sensitive settings protection
- [ ] Staff authorization enforcement
- [ ] Permission checks at business-logic level
- [ ] Audit trail for important changes
- [ ] Safe backup/export
- [ ] Account/session handling
- [ ] Privacy review for camera/OCR/product images
- [ ] Network/security review

---

# 16. UX / design-system quality

- [ ] Consistent typography
- [ ] Consistent spacing
- [ ] Consistent colors/tokens
- [ ] Consistent buttons/fields/cards
- [ ] Consistent dialogs/sheets
- [ ] Consistent scanner UI
- [ ] Consistent product forms
- [ ] Consistent empty states
- [ ] Consistent loading states
- [ ] Consistent error states
- [ ] Accessibility pass
- [ ] Large-touch-target pass
- [ ] Shopkeeper-speed UX pass
- [ ] Reduce unnecessary taps in billing

---

# 17. Repository quality

- [x] Professional README
- [x] Master roadmap
- [ ] Architecture documentation
- [ ] Product specification document
- [ ] Data model documentation
- [ ] Barcode/identifier specification
- [ ] Intelligent Capture specification
- [ ] Offline/sync specification
- [ ] Testing documentation
- [ ] Contribution/development guide
- [ ] Changelog
- [ ] Clean temporary artifacts
- [ ] Consistent naming conventions
- [ ] Consistent commit conventions
- [ ] No obsolete/dead code
- [ ] No duplicate implementations

---

# 18. Testing strategy — do before CI where possible

## Unit tests

- [ ] Money calculations
- [ ] Quantity calculations
- [ ] Tax calculations
- [ ] Discount calculations
- [ ] Checkout validation
- [ ] Inventory calculations
- [ ] Stock movement rules
- [ ] Barcode validation/normalization
- [ ] QR rejection rules
- [ ] SKU rules
- [ ] Identification confidence rules
- [ ] Candidate ranking
- [ ] Khata balance calculations
- [ ] Return/refund calculations

## Data/repository tests

- [ ] DAO CRUD
- [ ] Database constraints
- [ ] Database migrations
- [ ] Duplicate identifiers
- [ ] Transaction atomicity
- [ ] Cache behavior
- [ ] Offline behavior

## Integration tests

- [ ] Scan → product lookup
- [ ] Unknown barcode → intelligent identification
- [ ] Identification → product save
- [ ] Identification → return to POS
- [ ] POS → payment → sale → inventory deduction
- [ ] Credit sale → Khata
- [ ] Payment → Khata settlement
- [ ] Return → inventory restoration

## Physical-device/manual testing

- [ ] Camera permission
- [ ] Barcode scanning
- [ ] QR rejection
- [ ] Low light
- [ ] Different packaging/barcode sizes
- [ ] Fast repeated scans
- [ ] Offline billing
- [ ] Device rotation/configuration behavior
- [ ] Real printer/receipt workflow when supported

---

# 19. Release hardening

- [ ] Full unit-test pass
- [ ] Full repository/integration test pass
- [ ] Critical UI flow pass
- [ ] Physical-device pass
- [ ] Performance pass
- [ ] Memory/battery pass
- [ ] Security/privacy review
- [ ] Database migration review
- [ ] Backup/restore test
- [ ] Offline/recovery test
- [ ] Release build
- [ ] One deliberate CI verification cycle
- [ ] Production checklist
- [ ] Versioning/changelog

---

# 20. Definition of “done”

A feature is **not** done merely because its screen exists.

A feature is done when:

1. UI exists.
2. Business rules exist.
3. Data persistence exists where required.
4. Navigation/back behavior is correct.
5. Loading/error/empty states are handled.
6. It integrates with related workflows.
7. Important edge cases are covered.
8. Unit/repository tests exist where appropriate.
9. Offline behavior is considered.
10. Documentation/roadmap status is updated.
11. CI verification is performed later when the verification budget is available.

---

# 21. Current priority order

When choosing the next development task, follow this order unless a dependency requires otherwise:

1. **Clean architecture foundation**
2. **Complete product master + SKU/barcode system**
3. **Complete Intelligent Product Capture**
4. **Complete POS/billing**
5. **Inventory**
6. **Customers + Khata**
7. **Suppliers + purchasing**
8. **Reports + expenses**
9. **Staff/permissions + settings**
10. **Offline reliability**
11. **Cloud backup/sync**
12. **Hardening, testing and release**

Do not jump ahead just to create isolated UI screens. Prefer completing the end-to-end workflow of the current priority area.

---

# 22. CI budget rule

**GitHub Actions is scarce during the current development period.**

Until the available Actions budget returns:

- Do not use CI to discover obvious compile errors.
- Do not trigger verification runs for tiny changes.
- Do not create dummy commits to trigger workflows.
- Do not repeatedly rerun unexplained failures.
- Validate locally/static/unit-level wherever possible.
- Batch coherent work.
- Reserve CI for deliberate integration verification and release hardening.

This rule exists specifically to prevent development resources from being wasted on repeated CI experiments.
