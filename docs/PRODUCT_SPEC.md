# RetailGPT — Product Requirements Baseline

## Product intent
RetailGPT is a shopkeeper-first, offline-first Android retail operating system for Indian physical stores. The product optimizes the counter workflow: find product → add → review → charge, while keeping inventory, credit, purchasing and reporting trustworthy.

## Core user outcomes
- Complete an everyday sale with minimal taps and minimal typing.
- Identify known products immediately by search or barcode.
- Identify unknown products through guided capture without silently inventing identity.
- Maintain accurate stock, batches/expiry, customer credit and supplier payable balances.
- Continue core operations when connectivity is unavailable.
- Make business decisions from internally consistent sales, return, inventory and cash/payment data.

## Functional scope
### Sell / POS
Search, barcode scan, intelligent capture, voice entry, favorites/recent products, cart editing, hold/resume, discounts, price overrides, GST modes, cash, UPI, card, credit and split settlement. External electronic payments remain cashier-confirmed until a real provider/terminal integration is added.

### Product identity
Support retailer SKU, primary/alternate barcode and products without barcodes. Intelligent capture combines barcode, OCR, pack parsing and visual evidence, then produces a reviewable candidate. No weak visual guess may become a confirmed product automatically.

Capture target fields:
- product name
- brand
- flavor/variant when printed evidence supports it
- pack size/unit
- printed MRP
- barcode/GTIN when available
- category/unit hints with explicit confidence

### Inventory
Stock quantity, inventory movements, batches, expiry, FEFO, adjustments and recovery/reconciliation. Stock mutations must be atomic and auditable.

### Customers / Khata
Customer profiles, credit sales, payments, balances and return adjustments. Cross-store ownership must be enforced at mutation boundaries.

### Purchasing / Suppliers
Supplier master, purchase entry, paid/free quantities, schemes, batches/expiry, payable and supplier payments. Future supplier-system integrations must use a defined adapter boundary rather than direct UI coupling.

### Analytics
Net sales, items, COGS, gross profit, payment mix, returns, discounts, tax, receivables, supplier payables, inventory valuation and day-end reconciliation. Analytics must apply the same transaction definitions as operational workflows.

### Staff / authentication
Local owner/staff PIN foundation remains available. Planned account authentication supports Google and phone-number sign-in without making local POS operations network-dependent. Session state, account linking, sign-out and recovery must be explicit.

### Backup / sync
Encrypted local backup/restore is the current recovery foundation. Cloud sync is a later layer using queued, retryable operations, conflict handling and visible sync state. Local POS data remains authoritative while offline.

## Intelligent capture requirements
1. Prefer exact local barcode/catalog identity over generic visual labels.
2. Reject arbitrary QR payloads from normal product identity flow.
3. Aggregate evidence across multiple frames.
4. Filter obvious OCR metadata/background noise.
5. Require repeated printed identity evidence when barcode/catalog identity is unavailable.
6. Preserve retailer-controlled SKU, selling price, purchase price and stock unless explicitly edited.
7. Show evidence and confidence before confirmation.
8. Provide manual correction/fallback when evidence is weak.
9. Support difficult angles through multi-frame observation and robust camera lifecycle handling rather than assuming one perfect frame.
10. Record correction feedback so future matching can improve without silently mutating the catalog.

## UX goals
- Counter-first hierarchy; the primary action must be obvious.
- Thumb-friendly touch targets and minimal typing.
- Stable navigation/back behavior with no unexpected screen resets.
- Loading, empty, no-result, error, offline, syncing, conflict, low-stock, out-of-stock and confirmation states are intentional UI states.
- Avoid decorative AI panels, gradients and generic SaaS dashboard patterns.
- Use realistic Indian retail currency/payment language and examples.

## Performance targets
These are engineering acceptance targets, not claims about current performance:
- Search results: visible first results within 300 ms from local data under normal device load.
- Add-to-cart interaction: UI acknowledgement within 100 ms after local validation.
- Barcode detection: aim for a stable detection within 1 s under normal lighting when the product barcode is in frame.
- Intelligent capture: produce a reviewable evidence result within 3 s of adequate multi-frame evidence on supported mid-range devices.
- Checkout: commit the complete sale transaction atomically with no duplicate mutation on retry.
- Offline core flows: Product, POS, Khata and local purchasing remain functional without network access.
- Crash target: zero known crashes in the critical SELL/BUY/CREDIT acceptance suite.

## Reliability and data integrity
- All financial/stock mutations use deterministic business rules and transactional persistence.
- Idempotency must protect retried checkout/payment posting.
- Cross-store data references are invalid unless ownership matches the active store.
- Money rounding becomes centralized before any database-wide numeric migration.
- Restore validates schema/integrity before replacing live data.

## Authentication acceptance
The account layer must support Google and phone-number authentication, explicit loading/error states, session persistence and account recovery. Authentication may gate the cloud account, but opening the local retail database and performing permitted offline work must not depend on an active network request after the store is initialized.

## Supplier integration strategy
Define a provider-neutral supplier adapter with:
- supplier identity mapping
- product/SKU mapping
- price and availability import
- purchase/order submission hooks
- authentication/credential boundaries
- retry and idempotency rules
- mapping-error reporting

No provider-specific implementation should leak into POS or core inventory business rules.

## Definition of done
A product area is complete only when UI, business rules, persistence, navigation, loading/error/empty/offline states, edge cases, permissions, recovery behavior, tests and documentation are covered. A green build alone is insufficient; physical-device validation is required before release readiness.
