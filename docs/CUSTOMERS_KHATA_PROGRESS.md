# RetailPOS V2 — Customers & Khata Progress

## Implemented

- Customer list/search by name or phone.
- Add customer with duplicate-phone protection.
- Customer → Khata navigation wired to the current app shell.
- Live Khata balance from the Room ledger.
- Credit-sale ledger entries already created transactionally by checkout.
- Partial Khata payment collection.
- Full settlement when the final outstanding balance is collected.
- Overpayment prevention.
- Central `KhataRules` for payment validation and balance-state classification.
- Customer deletion blocked while a non-zero Khata balance exists.
- Transaction list displayed from the local ledger.

## Deliberately not claimed complete

- Credit limits.
- Payment receipts/share/print.
- Customer statement export.
- Customer purchase-history aggregation UI.
- Payment-method-specific collection records.
- Staff permission enforcement for Khata collection.
- Full integration/device testing.

## Data boundary

The existing Room schema already contains `customers` and `customer_ledger`. This slice does not require a schema migration.

Do not delete or mutate ledger history simply to remove a customer. A customer with a non-zero balance must be settled before deletion.
