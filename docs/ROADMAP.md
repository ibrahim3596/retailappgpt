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
- [x] Avoid throwaway CI-trigger commits and temporary verification files.
- [x] Keep the repository understandable and production-quality.
- [x] Update this roadmap whenever architecture or product scope is materially changed.

---

# 1. Product architecture & engineering foundation

## Application architecture

- [x] Android/Kotlin foundation
- [x] Jetpack Compose foundation
- [x] Room persistence foundation
- [x] CameraX foundation
- [x] ML Kit barcode/OCR foundation
- [ ] Finalize feature-oriented package structure
- [x] Establish `core / data / domain / feature / ui` boundaries (in progress; legacy files remain)
- [x] Keep business rules independent from Compose screens for identity/barcode rules
- [ ] Establish consistent ViewModel/UI-state pattern
- [x] Establish repository boundaries
- [ ] Add formal use-case layer across all major features
- [ ] Centralize shared design system
- [ ] Standardize loading/error/empty/success states
- [ ] Standardize navigation and back-stack behavior
- [x] Standardize core product validation rules
- [ ] Establish logging/diagnostic strategy

## Data integrity

- [x] Stable IDs for product/barcode/cache foundations
- [x] Database migration strategy foundation
- [ ] Referential integrity rules across all entities
- [ ] Transaction boundaries for sales/inventory/Khata
- [x] Product + primary barcode save protected by a Room transaction
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
- [x] Primary barcode
- [x] Multiple/alternate barcodes per product
- [x] Barcode type storage
- [x] GTIN/EAN/UPC identifier support
- [x] Identifier normalization
- [x] Identifier uniqueness rules
- [x] Duplicate identifier detection
- [x] Product-without-barcode support
- [x] Canonical barcode lookup through `product_barcodes`
- [x] Product search includes alternate barcodes

## Product information

- [ ] Product name
- [ ] Brand
- [ ] Category/subcategory
- [ ] Unit of measure
- [ ] Pack size / quantity per pack
- [ ] Variant support
- [ ] Product image
- [ ] Description/notes
- [x] Purchase price/cost foundation
- [x] Selling price foundation
- [x] MRP foundation
- [ ] Tax configuration
- [ ] Discount configuration
- [x] Minimum stock level foundation
- [ ] Active/inactive product state

## Product workflows

- [ ] Create product
- [ ] Edit product
- [ ] Duplicate/copy product
- [ ] Archive product
- [ ] Restore product
- [ ] Product search and filtering UI
- [ ] Category browsing
- [ ] Product detail page
- [ ] Bulk product import
- [ ] Bulk product export
- [ ] Product backup/restore

## Built-in product/catalog intelligence

- [x] Barcode → public catalog lookup foundation
- [x] Catalog candidate matching foundation
- [x] Catalog result review before applying
- [x] Store-specific overrides for retailer-controlled fields
- [x] Local cache of resolved product identities foundation
- [ ] Built-in/common product catalog dataset for offline/common items

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
- [x] Manual barcode entry fallback
- [x] Manual product search fallback

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
- [x] Persistent barcode-backed cache foundation
- [ ] Complete live persistent-identification integration audit
- [ ] Robust OCR-noise filtering integration
- [ ] Better candidate ranking
- [x] Product-name/brand extraction foundation
- [ ] Pack-size extraction
- [ ] MRP/price extraction where reliable
- [ ] Category inference
- [ ] Unit inference
- [ ] Variant detection
- [x] Image-label-assisted recognition foundation
- [ ] Dedicated image-assisted product recognition service
- [ ] Human correction/feedback loop

## Intelligent safety rules

- [x] Never silently turn a weak visual guess into a confirmed product
- [x] Show why a candidate was selected
- [x] Show confidence/evidence to the retailer
- [x] Allow retailer to reject the suggestion
- [x] Allow retailer to keep camera/OCR data instead of catalog data
- [x] Preserve retailer-controlled fields
- [x] Require review when confidence is insufficient

---

# 4. Barcode / QR scanning system

> Barcode scanning must be retail-aware. Not every code visible on packaging is a product identifier.

## Supported identification behavior

- [x] Detect common retail barcodes
- [x] Normalize scanned values
- [x] Validate barcode format/check digit where applicable
- [x] Resolve barcode against local products first through the canonical barcode table
- [x] Resolve unknown barcode through catalog when available
- [x] Offer intelligent identification for unknown products
- [x] Prevent duplicate barcode assignment
- [x] Support multiple barcodes on one product
- [x] Manual barcode entry fallback

## QR handling

- [x] Distinguish product barcodes from QR codes
- [x] Ignore/reject unsupported QR codes during normal product scanning
- [x] Do not create a product from an arbitrary QR payload
- [x] Explain that unsupported QR codes are not accepted by the product scanner
- [ ] Keep payment/business QR handling separate from product identification

## Scanner UX

- [x] Fast repeated scanning foundation
- [x] Duplicate-scan debounce
- [x] Scan success callback
- [ ] Unknown-code state polish
- [ ] Invalid-code state polish
- [x] Unsupported-code handling for QR
- [x] Camera permission handling
- [x] Flash/torch control
- [ ] Camera retry/restart handling
- [ ] Low-light guidance

---

# 5. POS / billing — core daily workflow

## Product entry

- [x] Cart foundation
- [x] Checkout rules foundation
- [ ] Fast product lookup
- [ ] Barcode scan → cart
- [x] Intelligent identification from unknown barcode foundation
- [x] Intelligent product capture directly from POS foundation
- [x] Newly identified product → save → return to active bill foundation
- [ ] Recently sold products
- [ ] Quick-add/favorites where appropriate

## Cart

- [x] Cart model foundation
- [ ] Add/remove item UI polish
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
- [x] Stock movement history foundation
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

- [x] Low-stock threshold foundation
- [ ] Low-stock alerts
- [ ] Out-of-stock state
- [x] Batch/lot data foundation
- [x] Expiry-date data foundation
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
- [x] Ledger data foundation
- [ ] Ledger UI/history
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
- [x] Product lookup works offline for locally stored products
- [ ] Inventory works offline end-to-end
- [ ] Customer/Khata works offline end-to-end
- [ ] Reports work from local data
- [x] Intelligent capture has graceful public-catalog fallback
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
- [x] Barcode validation/normalization foundation
- [x] QR rejection rules foundation
- [x] SKU rules foundation
- [x] Identification confidence rules
- [ ] Candidate ranking
- [ ] Khata balance calculations
- [ ] Return/refund calculations

## Data/repository tests

- [ ] DAO CRUD
- [ ] Database constraints
- [ ] Database migrations
- [ ] Duplicate identifiers
- [x] Product + primary barcode transaction path
- [ ] Full transaction atomicity audit
- [x] Identification cache behavior foundation
- [ ] Offline behavior

## Integration tests

- [ ] Scan → product lookup
- [x] Unknown barcode → intelligent identification foundation
- [x] Identification → product save foundation
- [x] Identification → return to POS foundation
- [ ] POS → payment → sale → inventory deduction
- [ ] Credit sale → Khata
- [ ] Payment → Khata settlement
- [ ] Return → inventory restoration

## Physical-device/manual testing

- [ ] Camera permission
- [ ] Barcode scanning
- [x] QR rejection behavior in product scanner
- [ ] Low light
- [ ] Different packaging/barcode sizes
- [x] Fast repeated-scan debounce foundation
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
