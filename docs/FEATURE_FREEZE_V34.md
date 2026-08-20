# RetailPOS V2 — Feature Freeze V34

Branch: `retailpos-v2`

## Development status

The core shopkeeper-first, offline-first retail product is now considered **feature-complete for the current V2 scope**. Approximate feature development: **90%+ of the planned core retail product**.

This is not a claim of release readiness. Build, migration, integration, physical-device, performance and regression verification remain intentionally pending.

## Core SELL loop

- Product search and canonical barcode lookup
- Primary/alternate barcodes
- Barcode validation and QR/payment payload rejection
- Intelligent unknown-product billing handoff
- Cart and decimal quantities
- Loose-goods voice billing
- Hindi default voice plus Indian language selection/model support
- Recently sold and favorites
- Item-level discount and price override controls
- Bill-level discounts
- Tax/GST treatment foundation
- Cash/card/UPI/credit and split-payment rules
- UPI app handoff foundation
- Checkout idempotency
- Atomic sale + stock deduction + COGS allocation
- Held bills
- Persistent active cart across process death
- Recovery validation for stale saved cart lines
- Receipts, sharing and reprints
- Returns/refunds foundation and credit reversal safety

## BUY loop

- Suppliers
- Purchases
- Purchase lines
- Free quantity/scheme economics
- Supplier payable ledger
- Inventory receiving
- Batch/expiry data
- FEFO batch allocation

## COLLECT loop

- Customers
- Khata/credit sales
- Partial/full payments
- Outstanding balances
- Supplier settlement foundations

## Owner/accounting

- Sales totals
- Payment reconciliation
- Split-payment normalization
- COGS
- Gross profit
- Operating result
- Expenses
- Customer receivables
- Supplier payables
- Inventory valuation/expiry exposure
- Day-end cash reconciliation

## Offline/recovery

- Local Room database
- Offline product lookup
- Identification cache fallback
- Held bills
- Durable active cart
- Persistent checkout idempotency
- Stale-cart preflight
- Store-safe encrypted backup foundation
- Backup checksum/schema/store validation

## Security/safety

- PIN authentication foundation
- Role-based permissions
- Business-layer discount/override/stock enforcement
- Direct-activity permission gates
- QR/payment payload isolation
- Intelligent-capture review/confidence safety
- AES-GCM encrypted backup container

## Remaining before release

1. Full command-line build in Colab/other build environment.
2. Full unit-test pass.
3. Full Room migration-chain verification through v25.
4. Integration tests for SELL/BUY/COLLECT/returns/backup.
5. Physical Android device test using ADB.
6. Offline/process-death torture testing.
7. Camera/microphone/voice/barcode testing under real shop conditions.
8. Performance, memory, accessibility and battery pass.
9. Printer/thermal receipt hardware pass.
10. Security/privacy/export/import review.
11. CI verification after the Actions budget is restored.

## Explicitly future platform work

Cloud sync, multi-device, multi-store, advanced computer-vision recognition datasets, demand forecasting and enterprise ERP capabilities remain outside this core feature freeze.

## CI constraint

Do not trigger GitHub Actions during the zero-minute period. No claim of CI-green is valid until a successful workflow exists for the exact commit.
