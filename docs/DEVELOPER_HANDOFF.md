# RetailPOS V2 — Developer Handoff & Continuation Guide

> **Purpose:** This document transfers the product, engineering, repository, workflow, and future-development context to any contributor or a future ChatGPT/Codex session.
>
> **Rule:** Read this document, `docs/ROADMAP.md`, `README.md`, and the relevant source files before changing code. The repository is the source of truth; do not trust an old chat summary over the actual code.

---

## 1. Project identity

**Project:** RetailPOS V2  
**Repository:** `ibrahim3596/retailappgpt`  
**Active branch:** `retailpos-v2`  
**Main branch:** previous/reference implementation  
**Target:** a serious shopkeeper-first, offline-first Android POS for everyday retail operations.

This is not intended to remain a demo. The product vision is a complete retail operating system covering product master, intelligent product identification, billing, inventory, customers/Khata, suppliers, reporting, staff permissions, settings, offline reliability, and later cloud backup/synchronization.

---

# 2. The most important product idea

## Intelligent Product Capture

The flagship differentiator is that the app should eventually identify a product intelligently instead of merely reading a barcode.

The intended user experience is:

```text
Shopkeeper points phone at product
              ↓
Camera + barcode + OCR + visual evidence
              ↓
Normalize / clean observations
              ↓
Generate product candidates
              ↓
Local product lookup first
              ↓
Catalog lookup when useful
              ↓
Combine evidence
              ↓
Confidence + explanation
              ↓
Shopkeeper reviews
              ↓
Product is created/updated
```

A barcode is only one signal. A product may have no barcode, a damaged barcode, multiple barcodes, or only printed/package information.

The app must **not blindly invent a product** from a weak visual guess. Weak recognition is a suggestion and requires review.

The app should preserve store-controlled information such as selling price, purchase price, stock, retailer SKU, tax configuration, and other local settings rather than silently replacing them with catalog information.

---

# 3. Barcode / SKU / QR philosophy

The product identity model intentionally distinguishes:

- retailer SKU
- primary barcode
- alternate/secondary barcodes
- barcode type/symbology
- GTIN/EAN/UPC-style global identifiers
- products with no barcode

The canonical barcode relationship is the `product_barcodes` table. The older `ProductEntity.barcode` field is currently retained as a compatibility mirror and should not become authoritative again.

Barcode behavior should follow:

```text
Scan barcode
    ↓
Normalize
    ↓
Validate when applicable
    ↓
Local canonical product_barcodes lookup
    ↓
Catalog fallback if unknown
    ↓
Intelligent identification if still unresolved
```

### QR rule

Normal product scanning should reject/ignore arbitrary QR payloads. A QR code must not automatically become a product identifier.

Payment/business QR functionality should remain separate from product identification.

There was an explicit design discussion about “rejecting QR”: retain this distinction when further developing the scanner.

---

# 4. Current architecture

The project uses:

- Kotlin
- Android SDK
- Jetpack Compose
- Room
- CameraX
- Google ML Kit

The intended architectural direction is:

```text
Compose UI
    ↓
ViewModel / UI state
    ↓
Use case / business rules
    ↓
Repository
    ↓
Room / network / device services
```

The repository is partway through the transition from older mixed/legacy structures toward feature-oriented responsibilities.

Preferred long-term structure:

```text
com.retailpos.app/
├── core/
│   ├── identifiers/
│   ├── products/
│   └── ...
├── data/
├── domain/
├── feature/
│   ├── products/
│   ├── billing/
│   ├── inventory/
│   ├── customers/
│   ├── reports/
│   └── ...
└── ui/
```

Do not perform a giant rewrite just for aesthetics. Refactor boundaries gradually while touching related functionality.

---

# 5. Current Product Master state

The Product Master is the current mature foundation and is substantially complete as a foundation.

It already contains foundations for:

- product entity
- product DAO
- SKU normalization/validation
- canonical barcode entity
- primary barcode
- alternate barcodes
- barcode type
- GTIN/EAN/UPC validation foundation
- duplicate identifier detection
- products without barcodes
- canonical barcode lookup
- alternate barcode search
- product list/search
- stock filters
- product create/edit
- dedicated product details
- product metadata
- category/subcategory
- pack size/unit
- description/notes
- image URI persistence
- image picker/preview/remove
- purchase price
- selling price
- MRP
- low-stock threshold
- catalog lookup foundation
- identification cache foundation

Important source areas include:

```text
app/src/main/java/com/retailpos/app/data/
  ProductEntity.kt
  ProductDao.kt
  ProductBarcodeEntity.kt
  ProductBarcodeDao.kt
  ProductRepository.kt
  ProductViewModel.kt
  ProductMetadataEntity.kt
  ProductMetadataDao.kt
  ProductMetadataRepository.kt
  ProductMetadataViewModel.kt
  ProductCatalogLookup.kt
  ProductIdentificationCache.kt
  RetailDatabase.kt
  DatabaseMigrations.kt

app/src/main/java/com/retailpos/app/core/
  identifiers/
  products/
  product/

app/src/main/java/com/retailpos/app/ui/
  screens/ProductListScreen.kt
  screens/ProductReviewScreen.kt
  screens/ProductMetadataScreen.kt
  components/ProductMetadataEditor.kt
```

Exact filenames and code should always be re-checked before editing because the repository can change between sessions.

---

# 6. Database state

The Room database is currently at **version 11**.

Migration history has been built sequentially. The recent metadata addition introduced the `10 → 11` migration and `product_metadata` table.

Do not change entity schemas without adding the corresponding migration or an explicit, deliberate database strategy.

Important existing tables include:

- `products`
- `product_barcodes`
- `product_metadata`
- `sales`
- `sale_lines`
- `inventory_movements`
- `inventory_batches`
- `customers`
- `customer_ledger`
- `product_identification_cache`

Keep data integrity and transaction boundaries as a first-class concern.

A previous bug risk was identified where a product could theoretically save without its barcode if those writes were separate. Product + primary barcode save was therefore moved toward a Room transaction. Preserve that principle.

A future improvement is to save product + metadata in one atomic transaction where appropriate.

---

# 7. Existing application flows

The app already has navigation for major areas including:

```text
Home
POS
Checkout
Receipt
Products
Add Product
Edit Product
Product Details
Billing Scanner
Inventory
Inventory Detail
Inventory Adjustment
Inventory Receive
Customers
Customer Khata
Analytics
Settings
```

The POS already has foundations for:

- cart
- checkout rules
- payment flow foundations
- sale persistence
- sale idempotency key
- receipt flow
- inventory adjustment/receive flows
- unknown barcode dialog
- intelligent-product-identification entry point

However, many of these areas are still foundations rather than release-ready end-to-end features.

---

# 8. Unknown product flow

The intended flow when a barcode is scanned in billing and no local product is found is:

```text
Billing scanner
   ↓
Unknown barcode
   ↓
IDENTIFY PRODUCT
   OR
ADD MANUALLY
```

The intelligent option should eventually:

1. open intelligent capture
2. scan/read the product
3. prefill barcode and relevant product fields
4. show evidence/confidence
5. allow catalog candidate review
6. allow reject/keep-camera decision
7. save the product
8. return to the active bill
9. add the newly created product to the cart

The return-to-POS flow has a foundation but must be verified end-to-end later.

---

# 9. Current development priority

Use this order unless a dependency requires otherwise:

### Priority 1 — Finish Intelligent Product Capture

Complete:

- live persistent identification cache integration
- OCR-noise filtering
- better candidate ranking
- product name/brand extraction refinement
- pack-size extraction
- reliable MRP/price extraction where possible
- category inference
- unit inference
- variant detection
- image-assisted product recognition service
- human correction/feedback loop
- offline/common-product fallback strategy
- catalog source priority/freshness
- robust confidence explanation
- camera retry and low-light behavior

### Priority 2 — POS/Billing end-to-end

Complete:

- fast canonical lookup
- barcode scan → cart
- search → cart
- quantity editing
- discounts
- tax
- payment methods
- UPI
- split payment if required
- amount tendered/change
- sale persistence
- atomic inventory deduction
- receipts
- reprint
- held bills
- returns/refunds

### Priority 3 — Inventory hardening

Then:

- stock views
- stock-in/out
- adjustments
- movement audit
- suppliers/purchasing
- low-stock alerts
- expiry alerts
- FEFO/FIFO where applicable
- inventory valuation
- transfers

### Priority 4 — Customers + Khata

Complete the customer/credit workflow end-to-end.

### Priority 5 — Suppliers/purchasing

### Priority 6 — Reports + expenses

### Priority 7 — Staff/permissions + settings

### Priority 8 — Offline reliability

### Priority 9 — Cloud backup/sync

### Priority 10 — Full testing/hardening/release

Do not jump to a random UI screen when the current priority has an unfinished end-to-end dependency.

---

# 10. Future ideas already envisioned

These are ideas to preserve even if they are not immediate priorities.

## Intelligent Product Capture future ideas

- multi-frame product capture
- visual front-of-pack recognition
- OCR cleanup and field extraction
- candidate ranking from multiple evidence sources
- confidence calibration based on historical corrections
- retailer feedback loop
- image-assisted product recognition model/service
- common-product offline dataset
- catalog source priority and freshness model
- intelligent duplicate-product warnings
- packaging/variant recognition
- “this looks like an existing product” suggestions

## Product Master future ideas

- SKU generator
- duplicate/copy product
- product archive/restore
- variants
- units and conversions
- bulk import/export
- CSV/Excel import
- product backup/restore
- category hierarchy
- category filters
- sort options
- recently updated/recently sold

## POS future ideas

- favorites/quick add
- recently sold products
- held bills
- customer selection inside bill
- quick Khata sale
- split payments
- receipt printer integration
- thermal printer support
- reprint
- return/refund workflow
- keyboard/hardware scanner support

## Inventory future ideas

- supplier integration
- purchase orders
- purchase invoices
- batch and expiry dashboards
- automatic low-stock suggestions
- stock transfer
- inventory valuation
- FEFO/FIFO rules
- multi-location support

## Business intelligence future ideas

- daily owner dashboard
- gross profit
- COGS
- product profitability
- slow-moving inventory
- dead-stock detection
- demand forecasting
- reorder suggestions
- customer buying patterns
- category trends

## Cloud/future platform

- cloud backup
- multi-device sync
- multi-store support
- conflict resolution
- staff accounts
- web dashboard later
- remote reports
- secure business identity

These are future ideas, not permission to skip the current roadmap.

---

# 11. Important mistakes from previous development

A future contributor should explicitly avoid repeating these.

### GitHub Actions was used as a debugger

Earlier, repeated CI runs were triggered for unexplained failures. This consumed the user's GitHub Actions budget.

**Do not repeat this.**

The user currently has **zero Actions minutes available** and expects the budget to return around **September 13, 2026**.

Until then:

- do not trigger CI just to see whether code compiles
- do not rerun failed jobs repeatedly
- do not create dummy commits to wake CI
- do not create temporary verification PRs
- do not use GitHub Actions as the debugging loop
- inspect code carefully first
- add unit tests/static reasoning where practical
- batch coherent work
- reserve CI for deliberate integration/release verification after the budget returns

### Do not falsely claim a build is green

Only say CI/build verification is green when an actual successful result exists for the exact relevant commit.

### Do not assume previous assistant claims are necessarily correct

Some previous tool operations failed, conflicted, or were blocked. The repository itself is the source of truth.

Before continuing, inspect the actual files and recent commits and reconcile the roadmap with reality.

---

# 12. Development workflow expected from future contributors

For each coherent feature slice:

```text
1. Read roadmap
2. Inspect existing implementation
3. Identify dependencies and affected files
4. Design the smallest coherent architecture
5. Implement the slice
6. Add/update unit tests where possible
7. Inspect for obvious Kotlin/Room/Compose errors
8. Update docs/roadmap status
9. Commit with a clear message
10. Continue to the next dependent slice
```

When CI becomes available again:

```text
Feature batch
   ↓
Local/static/unit validation
   ↓
One deliberate CI run
   ↓
Fix if necessary
   ↓
One deliberate rerun
   ↓
Green
```

Do not create a CI loop.

---

# 13. Repository cleanliness rules

The repository should look like a real product project.

Avoid:

- temporary verification files
- dummy commits
- throwaway PRs
- duplicate implementations
- abandoned legacy code
- business logic buried in Compose screens
- arbitrary hard-coded business rules scattered across UI

Keep documentation synchronized.

Important docs:

```text
README.md

docs/ROADMAP.md
docs/DEVELOPER_HANDOFF.md
```

Future docs should include:

```text
docs/ARCHITECTURE.md
docs/PRODUCT_SPEC.md
docs/DATA_MODEL.md
docs/BARCODE_SYSTEM.md
docs/INTELLIGENT_CAPTURE.md
docs/OFFLINE_FIRST.md
docs/TESTING.md
docs/CONTRIBUTING.md
docs/CHANGELOG.md
```

---

# 14. Definition of done

A feature is not complete because a screen exists.

It is complete when:

1. UI exists.
2. Business rules exist.
3. Persistence exists when required.
4. Navigation/back behavior is correct.
5. Loading/error/empty states are handled.
6. Edge cases are considered.
7. It integrates with adjacent flows.
8. Appropriate tests exist.
9. Offline behavior is considered.
10. Documentation/roadmap is updated.
11. Later, when Actions are available, deliberate CI verification succeeds.

---

# 15. How a new ChatGPT/Codex session should start

A new development session must NOT immediately start editing files.

First do this:

```text
1. Inspect repository metadata and active branch.
2. Read README.md.
3. Read docs/ROADMAP.md completely.
4. Read docs/DEVELOPER_HANDOFF.md completely.
5. Inspect the latest commits on retailpos-v2.
6. Inspect the current source files relevant to the roadmap's active priority.
7. Identify what is actually implemented versus what the documentation claims.
8. Reconcile the roadmap if reality differs.
9. Only then choose the next coherent feature slice.
10. Do NOT use GitHub Actions unless explicitly asked and there is sufficient budget.
```

The new session should report a concise **state audit** before making code changes:

```text
Repository state:
Active branch:
Latest commit:
Current roadmap priority:
Verified implemented pieces:
Suspected/incomplete pieces:
Next feature slice:
CI status/budget constraint:
```

Then proceed with development.

---

# 16. Handoff principle

The long-term goal is not to preserve the exact implementation decisions of one contributor. The goal is to preserve the **product intent, architectural boundaries, important safety rules, roadmap, and development discipline**.

A future contributor may refactor implementation details when justified, but must not silently remove:

- offline-first core retail operation
- canonical SKU/barcode separation
- multiple-barcode support
- QR/product-code separation
- intelligent product identification
- evidence/confidence review
- retailer control over price/stock
- transactional data integrity
- clean repository practices
- deliberate CI usage

When a new architectural decision materially changes one of these assumptions, update `docs/ROADMAP.md` and this handoff document.

---

# 17. Immediate next action

**Finish Intelligent Product Capture before declaring Product Master fully production-ready.**

Start by auditing the existing implementation, especially:

- `ProductCatalogLookup`
- `ProductIdentificationCache`
- `IntelligentProductCaptureScreen`
- `ProductReviewScreen`
- barcode scanner flow
- OCR handling
- confidence/evidence logic
- unknown-product POS flow
- return-to-POS flow

Then implement the highest-value missing slice from the roadmap without using GitHub Actions.

After Intelligent Capture is coherent and locally validated, move into POS/billing end-to-end.
