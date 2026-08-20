# Customers + Khata Progress

## Completed

- Customer search by name/phone
- Customer creation with duplicate phone protection
- Customer → Khata navigation wired into the main app shell
- Live outstanding balance
- Credit-sale ledger integration already backed by Room
- Partial payment and full settlement validation
- Overpayment rejection
- Central Khata business rules
- Customer deletion blocked while an outstanding balance exists
- Khata transaction history
- Shareable customer statement
- Statement formatter and unit test
- Existing sale/credit data remains local/offline

## Deliberate boundaries

- Customer purchase-history UI is not yet a dedicated screen; existing sale data can be queried but is not exposed as a full history view yet.
- Payment receipt/reprint for Khata collections is not yet a dedicated artifact.
- Credit limits are not implemented.
- Supplier/purchasing persistence is not implemented because the Room database registration path is currently blocked for schema changes. Do not fake supplier persistence in UI-only state.

## Next dependencies

1. Dedicated customer purchase history
2. Khata payment receipt
3. Credit limits / staff permissions
4. Supplier persistence after a safe Room migration path is available
5. Purchase entry → batch receiving → inventory valuation/COGS
