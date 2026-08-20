# RetailPOS V2 — Developer & Contributor Guide

> **Read this before changing the project.**
>
> This document explains the product vision, architecture, development process, feature contracts, future direction, and rules for extending RetailPOS V2 safely. `docs/ROADMAP.md` is the execution checklist; this document explains the reasoning behind it.

---

## 1. What are we building?

RetailPOS V2 is a **shopkeeper-first, offline-first Android retail POS**.

The app is intended to cover the complete daily retail loop:

```text
Discover / create product
        ↓
Identify product
        ↓
Manage price + stock
        ↓
Scan/search during billing
        ↓
Create sale
        ↓
Collect payment
        ↓
Deduct inventory
        ↓
Receipt
        ↓
Customer / Khata when credit is used
        ↓
Reports / business insights
```

The product should feel fast enough for a real shop counter. Correctness matters more than adding decorative features quickly.

### Core product principles

1. **Offline first:** selling and core store operations must continue without a live network.
2. **Fast counter UX:** minimize taps, typing and waiting during billing.
3. **Data integrity:** money, stock, sales and Khata must never silently diverge.
4. **Intelligent, not reckless:** AI/product recognition should make suggestions with evidence and confidence, never silently invent facts.
5. **Retailer control:** the shopkeeper owns store-specific price, stock, SKU and operational data.
6. **Composable architecture:** business rules should be reusable by UI, import, scanner, AI and future sync layers.
7. **Build end-to-end:** a feature is not complete because a screen exists; its data, rules, persistence, navigation and integration must work together.

---

## 2. Source-of-truth documents

When working on the project, use these in this order:

| Document | Purpose |
|---|---|
| `docs/ROADMAP.md` | Master feature checklist and priority order |
| `docs/DEVELOPER_GUIDE.md` | This document: architecture, workflow, context and future direction |
| `README.md` | Public-facing repository overview |
| `docs/ARCHITECTURE.md` | Add/update when architectural decisions become substantial |
| `docs/PRODUCT_SPEC.md` | Add/update for detailed product behavior |
| `docs/DATA_MODEL.md` | Add/update for entity/relationship decisions |
| `docs/BARCODE_SYSTEM.md` | Add/update for identifier/scanning behavior |
| `docs/INTELLIGENT_CAPTURE.md` | Add/update for product-recognition behavior |
| `docs/OFFLINE_FIRST.md` | Add/update for offline/sync behavior |
| `docs/TESTING.md` | Add/update for test strategy |

If this guide and the roadmap disagree, **inspect the current implementation and then update the documents together**. Do not silently choose a new product direction.

---

## 3. Current repository context

### Active branch

`retailpos-v2` is the active V2 development branch.

`main` is the previous/reference implementation.

### Technology direction

- Kotlin
- Jetpack Compose
- Android SDK
- Room
- CameraX
- Google ML Kit
- Gradle

### Current high-level layers

```text
com.retailpos.app
├── core/       Shared rules, identifiers, product logic
├── data/       Room entities, DAOs, repositories, ViewModels
├── ui/         Compose screens/components/theme
└── MainActivity.kt / navigation
```

The exact package structure can evolve. The important rule is **responsibility separation**, not preserving a particular folder name.

---

## 4. Architecture rules

Use this dependency direction:

```text
UI
 ↓
ViewModel / UI state
 ↓
Use cases / business rules
 ↓
Repositories
 ↓
Local database / device services / network services
```

### UI

UI should:

- render state;
- collect state;
- emit user actions;
- handle navigation;
- show validation/loading/error states.

UI should **not** contain database transactions or important business rules.

### ViewModels

ViewModels coordinate UI state and user actions.

They should not become giant god objects. When a rule is reusable or complex, move it to `core` or a use-case/domain layer.

### Business rules

Rules such as:

- barcode normalization;
- SKU validation;
- tax calculation;
- discount calculation;
- stock validation;
- Khata balance calculation;
- identification confidence;
- product candidate ranking

should be testable without Compose or Android UI.

### Repositories

Repositories are the boundary between application logic and storage/services.

Prefer:

```text
Screen → ViewModel → Repository
```

over:

```text
Screen → DAO
```

Complex transactions belong in the data layer/repository/transaction boundary, not in composables.

---

## 5. Product identity model

This is one of the most important architectural decisions.

A retailer SKU and a manufacturer/global barcode are **not the same thing**.

Conceptually:

```text
Product
 ├── retailer SKU
 ├── primary barcode
 ├── alternate barcode #1
 ├── alternate barcode #2
 └── ...
```

The canonical barcode relationship is the `product_barcodes` system. The older single `ProductEntity.barcode` field exists only as a compatibility mirror during migration and must not become the source of truth again.

### Rules

- Normalize identifiers before comparison.
- Do not silently assign an existing barcode to another product.
- Validate GTIN check digits when applicable.
- Allow retailer-specific/non-GTIN identifiers where the business model requires them.
- Keep QR payloads separate from product identifiers.
- Product search should resolve alternate barcodes.
- Unknown barcode should flow into intelligent/manual identification rather than creating a random product.

---

## 6. Intelligent Product Capture

This is a flagship capability.

The intended experience is:

```text
Point camera at product
        ↓
Barcode + OCR + visual evidence
        ↓
Normalize signals
        ↓
Generate candidates
        ↓
Catalog lookup / local history
        ↓
Evidence aggregation
        ↓
Confidence score
        ↓
Show suggested identity
        ↓
Retailer confirms/corrects
        ↓
Save product
```

### Important distinction

The system should not claim:

> “This is definitely Product X.”

when the evidence only supports:

> “This looks like Product X with medium confidence.”

### Evidence hierarchy

Prefer stronger evidence in roughly this order:

1. exact local barcode match;
2. exact trusted catalog identifier match;
3. strong identifier + name/brand agreement;
4. strong OCR name/brand + catalog match;
5. visual/product-label evidence;
6. weak visual guess.

A weak visual result should never silently overwrite retailer-controlled fields.

### Retailer-controlled fields

AI/catalog data may suggest:

- product name;
- brand;
- category;
- pack size;
- unit;
- identifiers.

The retailer remains authoritative for:

- store SKU;
- selling price;
- purchase price;
- current stock;
- minimum stock threshold;
- store-specific notes.

---

## 7. Barcode scanning behavior

Scanning is used in multiple contexts, so do not build one giant scanner that behaves identically everywhere.

### Billing scanner

```text
Scan
 ↓
Resolve local product
 ↓
Found → add to cart
Unknown → identify/add product
```

### Product-management scanner

```text
Scan
 ↓
Resolve existing product
 ↓
Found → show product
Unknown → prefill product identity
```

### QR handling

A QR code is not automatically a product barcode.

Normal product scanning should:

- distinguish product barcode from QR;
- reject/ignore unsupported QR payloads;
- avoid creating products from arbitrary QR content;
- keep payment/business QR handling separate.

---

## 8. Product Master

The Product Master is the authoritative store catalog.

It should eventually support:

- name;
- brand;
- SKU;
- primary/alternate barcodes;
- GTIN/EAN/UPC;
- category/subcategory;
- unit;
- pack size;
- variant;
- image;
- description;
- cost price;
- selling price;
- MRP where applicable;
- tax;
- discount rules;
- stock threshold;
- active/archive state.

### Product workflow

```text
Product list
 ↓
Search/filter/category
 ↓
Product detail
 ↓
Edit
 ├── identity
 ├── pricing
 ├── metadata
 ├── barcodes
 └── image
```

A product without a barcode is valid. The app must not force a fake barcode.

---

## 9. POS workflow

The POS is the most latency-sensitive part of the app.

Target flow:

```text
New bill
 ↓
Scan/search
 ↓
Cart
 ↓
Discount/tax
 ↓
Payment
 ↓
Atomic sale transaction
 ↓
Inventory deduction
 ↓
Receipt
```

### Sale transaction rule

Sale creation and inventory deduction must be treated as one logical transaction.

Do not create a successful sale while leaving stock unchanged.

Do not deduct stock while failing to record the sale.

Use database transaction boundaries and idempotency for payment/checkout retries.

---

## 10. Inventory

Inventory should be movement-driven, not just a number that screens overwrite.

Conceptually:

```text
Opening stock
 + purchases/receipts
 + positive adjustments
 - sales
 - returns/negative adjustments
 ± transfers
 = current stock
```

Important future records:

- stock movement;
- reason;
- timestamp;
- actor/staff member;
- source transaction;
- batch/lot;
- expiry where applicable.

Never hide a stock correction by silently changing the quantity without an auditable reason.

---

## 11. Customers & Khata

Khata is a proper financial ledger, not simply a `balance` field.

Conceptually:

```text
Credit sale       → debit customer
Customer payment  → credit customer
Adjustment        → explicit ledger event
```

The displayed outstanding balance should be derived from valid ledger entries or a transactionally maintained equivalent.

Credit sale and inventory deduction must remain consistent.

---

## 12. Suppliers & purchasing

Supplier/purchase support exists to feed inventory correctly.

Future flow:

```text
Supplier
 ↓
Purchase invoice
 ↓
Purchase lines
 ↓
Cost/batch/expiry
 ↓
Stock receipt
 ↓
Inventory movement
```

Do not implement supplier screens without connecting them to actual inventory behavior.

---

## 13. Reports

Reports should be derived from authoritative transactional data.

Avoid maintaining multiple manually updated totals when they can be calculated from sales, inventory, expenses and Khata records.

Core reports:

- daily sales;
- sales by date range;
- payment-method breakdown;
- best sellers;
- slow movers;
- inventory valuation;
- low stock;
- expiry;
- customer outstanding;
- collections;
- gross profit;
- expenses.

---

## 14. Offline-first strategy

Core workflows must work without internet:

- billing;
- product search;
- product creation;
- inventory;
- customer/Khata;
- reports from local data.

Network services should enhance the app rather than make basic retail operation impossible.

For intelligent capture, offline fallback should still allow:

- barcode detection;
- OCR;
- local catalog/cache matching;
- manual correction.

Cloud/catalog services can improve identification when connectivity exists.

---

## 15. Future cloud/sync direction

Cloud comes **after** local correctness.

Future architecture:

```text
Local DB
   ↓
Outbox / sync queue
   ↓
Cloud API
   ↓
Remote store data
```

Eventually support:

- authentication;
- business/store identity;
- cloud backup;
- restore;
- multi-device sync;
- conflict detection;
- conflict resolution;
- sync status;
- failed-sync retry.

Do not add cloud dependencies to core workflows merely because they are available.

---

## 16. Development workflow

### Normal feature process

```text
1. Read roadmap
2. Inspect existing implementation
3. Identify dependencies
4. Design data/business/UI boundaries
5. Implement the smallest complete slice
6. Add/update tests
7. Integrate with existing workflows
8. Update documentation
9. Review for duplicate/legacy implementations
10. Only then use CI when available
```

### What “smallest complete slice” means

For example, do not create only a “Suppliers” screen.

Instead:

```text
Supplier entity
 → DAO
 → repository
 → supplier form
 → supplier list
 → purchase association
 → inventory receipt
 → validation
 → tests
```

The slice can still be developed in several commits, but it should represent one coherent feature.

---

## 17. CI / GitHub Actions policy

GitHub Actions is a **verification gate**, not the primary debugger.

When CI resources are limited:

- do not trigger CI for every tiny change;
- do not create dummy commits to trigger workflows;
- do not repeatedly rerun unexplained failures;
- do not use CI to discover obvious Kotlin syntax/type errors;
- batch coherent changes;
- perform local/static/unit validation first;
- reserve CI for deliberate verification.

When CI becomes available again, the preferred flow is:

```text
Large coherent feature batch
        ↓
Local validation
        ↓
One CI run
        ↓
Fix actual failures
        ↓
One verification run after fixes
```

CI should eventually include:

- compile;
- unit tests;
- lint/static analysis;
- relevant integration tests;
- release build verification.

Use workflow concurrency/cancellation so obsolete runs do not waste resources.

---

## 18. How to validate without CI

Before pushing a feature for CI verification, reason through:

### Compile safety

- imports;
- package names;
- type signatures;
- nullable/non-nullable values;
- Compose state APIs;
- Room annotations;
- migration versions;
- navigation route arguments;
- sealed/enum/class hierarchy correctness.

### Data safety

- duplicate identifiers;
- transaction boundaries;
- migrations;
- null/default behavior;
- existing-data compatibility;
- store scoping.

### UX safety

- loading state;
- empty state;
- error state;
- back navigation;
- cancellation;
- repeated taps;
- offline behavior.

### Feature integration

Ask:

> “What happens immediately before and immediately after this feature?”

If those flows break, the feature is not finished.

---

## 19. Database migration rules

Every schema change must answer:

1. What is the new schema?
2. What happens to existing data?
3. Is there a migration from the current version?
4. Are defaults safe?
5. Can old rows violate new constraints?
6. Does the application version increment correctly?

Never casually use destructive migration for a production-like retail database.

A shop's products, sales, stock and Khata are valuable data.

---

## 20. Naming and code-quality rules

Prefer names that describe business meaning.

Good:

```text
ProductIdentifierValidator
CreateSaleUseCase
InventoryMovementReason
ProductMetadataRepository
```

Avoid vague names:

```text
Helper
Manager2
Temp
UtilsFinal
NewScreen
TestThing
```

Temporary experiments should not be left in product code.

Do not duplicate the same business rule in multiple files when a shared rule can be extracted.

---

## 21. UI quality rules

RetailPOS is not a generic consumer app.

Optimize for:

- fast scanning;
- one-handed use;
- large touch targets;
- readable totals;
- clear errors;
- minimal typing;
- obvious primary action;
- quick recovery from mistakes.

Do not sacrifice counter speed for visual decoration.

The design system should remain consistent across POS, products, inventory, customers and settings.

---

## 22. Security and financial integrity

Never trust UI restrictions alone.

If a cashier cannot perform an operation, the permission must eventually be enforced at the business-logic/data boundary as well.

Important operations requiring eventual auditability:

- price overrides;
- discounts;
- refunds;
- stock adjustments;
- credit adjustments;
- payment changes;
- staff permissions;
- deletion/archive of important records.

---

## 23. Future ideas backlog

These ideas are intentionally not all part of the immediate MVP. Evaluate them only after core retail workflows are stable.

### Intelligence

- Product recognition from packaging images
- Better multi-frame recognition
- Brand/logo detection
- Product variant detection
- OCR correction using catalog context
- Personalized product candidate ranking
- Retailer feedback loop
- Duplicate-product merge suggestions
- Automatic category suggestions
- Smart pack-size/unit extraction
- Intelligent price/MRP extraction

### POS

- Favorites/quick-sale products
- Recently sold products
- Held bills
- Split payments
- Keyboard/external scanner optimization
- Customer-facing display
- Barcode label generation/printing
- Price labels
- Receipt printer integration

### Inventory

- Purchase-order workflow
- Supplier payable ledger
- Batch/expiry automation
- FEFO recommendations
- Reorder suggestions
- Dead-stock detection
- Stock forecasting
- Multi-location inventory

### Customer growth

- Loyalty points
- Customer segmentation
- Offers
- Purchase-based recommendations
- WhatsApp/shareable statements where legally/technically appropriate
- Payment reminders

### Business intelligence

- Sales forecasting
- Profit trends
- Anomaly detection
- Cash-flow view
- Product profitability ranking
- Suggested reorder quantities
- “What changed today?” dashboard

### Platform

- Multi-store support
- Multi-device sync
- Cloud backup
- Role-based access
- Web dashboard
- Admin portal
- API integrations

### Hardware

- Bluetooth barcode scanners
- Thermal printers
- Cash drawer integration
- Customer displays
- Weighing-scale integration

---

## 24. Features deliberately deferred

Do not let attractive future ideas destabilize the core build.

The following should generally wait until the foundation is reliable:

- complex cloud sync;
- multi-store architecture;
- advanced AI model hosting;
- loyalty systems;
- large analytics dashboards;
- external integrations;
- hardware-specific optimizations.

First make this loop extremely reliable:

```text
Product → Scan → Cart → Payment → Sale → Stock → Receipt
```

Then add complexity.

---

## 25. Definition of done

A feature is complete only when:

- [ ] UI exists
- [ ] data model exists where needed
- [ ] persistence exists where needed
- [ ] business rules are separated from UI
- [ ] validation exists
- [ ] loading/error/empty states exist
- [ ] navigation works
- [ ] related workflows work
- [ ] existing data is preserved
- [ ] migrations exist when required
- [ ] tests exist where appropriate
- [ ] offline behavior is considered
- [ ] documentation is updated
- [ ] roadmap status is updated
- [ ] CI verification is performed later when the verification budget is available

---

## 26. Recommended development order

Follow the master roadmap unless a dependency requires otherwise:

```text
1. Architecture foundation
        ↓
2. Product Master
        ↓
3. Intelligent Product Capture
        ↓
4. POS / Billing
        ↓
5. Inventory
        ↓
6. Customers + Khata
        ↓
7. Suppliers + Purchasing
        ↓
8. Reports + Expenses
        ↓
9. Staff + Permissions + Settings
        ↓
10. Offline reliability
        ↓
11. Cloud backup + Sync
        ↓
12. Testing + Hardening
        ↓
13. Release
```

If a feature depends on another incomplete subsystem, finish the dependency instead of building a disconnected mock.

---

## 27. Handoff procedure for a new contributor

A new contributor should:

1. Read `README.md`.
2. Read `docs/ROADMAP.md`.
3. Read this `docs/DEVELOPER_GUIDE.md`.
4. Inspect the relevant existing entities/DAOs/repositories/screens before changing anything.
5. Search the repository for existing implementations of the same concept.
6. Identify whether the feature changes the database.
7. Identify whether the feature changes navigation.
8. Identify which existing workflows consume the feature.
9. Implement a complete slice.
10. Add tests/rules where appropriate.
11. Update roadmap/documentation.
12. Do not trigger CI unnecessarily.

### Before handing work to another contributor

Document:

- what was implemented;
- what remains;
- important architectural decisions;
- known limitations;
- database changes;
- expected next step;
- any unverified assumptions.

Never leave context only in chat messages.

---

## 28. Current project status

The V2 branch already contains foundations for:

- product persistence;
- product/barcode relationships;
- barcode validation;
- catalog lookup;
- intelligent capture foundations;
- inventory;
- POS/cart/checkout foundations;
- customers/Khata foundations;
- product metadata;
- repository documentation.

The project is still in active development. **Do not assume an existing screen means the entire underlying feature is production-ready.** Follow the roadmap and inspect the data/business layer.

---

## 29. Final rule

When unsure what to do next:

> **Do not invent a new direction. Read `docs/ROADMAP.md`, inspect the current implementation, finish the highest-priority incomplete end-to-end slice, and update the documentation.**

The goal is not maximum code volume.

The goal is a reliable retail product that another developer can understand and safely continue building.
