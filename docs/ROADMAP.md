# RetailPOS V2 Roadmap

This roadmap is the working map for the V2 rebuild. Features should be implemented in coherent slices and integrated into existing workflows rather than developed as isolated screens.

## 1. Core foundation

- [x] Android project foundation
- [x] Compose UI foundation
- [x] Room persistence foundation
- [x] Product/barcode data model foundation
- [ ] Finalize package/module boundaries
- [ ] Centralize shared design-system components
- [ ] Establish consistent error/loading/empty states

## 2. Product management

- [x] Product entities and DAO foundation
- [x] Barcode entities and DAO foundation
- [x] SKU/product identifier foundation
- [ ] Product create/edit flow
- [ ] Product search and filtering
- [ ] Categories and units
- [ ] Multiple identifiers per product
- [ ] Product import/export
- [ ] Duplicate-product detection

## 3. Intelligent Product Capture

- [x] Barcode scanning foundation
- [x] OCR foundation
- [x] Visual-label foundation
- [x] Catalog lookup foundation
- [x] Identification evidence/confidence model
- [x] Review-before-save principle
- [ ] Complete persistent identification integration
- [ ] Robust OCR-noise filtering integration
- [ ] Better product candidate ranking
- [ ] Image-assisted product recognition service
- [ ] Human correction/feedback loop

## 4. POS / billing

- [x] Cart foundation
- [x] Checkout rules foundation
- [ ] Fast product lookup
- [ ] Barcode-to-cart flow
- [ ] Quantity editing
- [ ] Discounts
- [ ] Taxes
- [ ] Payment methods
- [ ] Sale persistence
- [ ] Receipt generation
- [ ] Returns/refunds
- [ ] Held/suspended bills

## 5. Inventory

- [x] Inventory data foundation
- [ ] Stock-in
- [ ] Stock-out
- [ ] Stock adjustments
- [ ] Purchase entry
- [ ] Supplier support
- [ ] Stock transfers
- [ ] Low-stock alerts
- [ ] Batch/lot tracking
- [ ] Expiry tracking
- [ ] Inventory valuation
- [ ] Stock movement history

## 6. Customers & Khata

- [x] Customer data foundation
- [x] Khata data foundation
- [ ] Customer management UI
- [ ] Credit sale flow
- [ ] Payment collection
- [ ] Ledger/history
- [ ] Outstanding balance calculations
- [ ] Customer statements

## 7. Reports

- [ ] Daily sales
- [ ] Sales by period
- [ ] Gross profit
- [ ] Product performance
- [ ] Inventory report
- [ ] Low-stock report
- [ ] Customer credit report
- [ ] Expense tracking
- [ ] Exportable reports

## 8. Store, staff & settings

- [ ] Store profile
- [ ] Tax configuration
- [ ] Receipt configuration
- [ ] Staff accounts
- [ ] Role/permission model
- [ ] Audit history
- [ ] Backup/restore
- [ ] Data export

## 9. Reliability & sync

- [ ] Offline-first audit
- [ ] Safe database migrations
- [ ] Conflict strategy
- [ ] Cloud authentication
- [ ] Cloud backup
- [ ] Multi-device sync
- [ ] Sync status and recovery

## 10. Release hardening

- [ ] Full unit-test pass
- [ ] Repository/integration tests
- [ ] Critical UI flow tests
- [ ] Physical-device testing
- [ ] Barcode/camera testing
- [ ] Performance pass
- [ ] Security/privacy review
- [ ] Release build
- [ ] CI verification
- [ ] Production checklist
