# RetailPOS V2 — POS Pricing Progress

The checkout pricing slice now has a local, transactional pricing path.

## Implemented

- Store GST mode persisted in Room
- Store modes: NO_GST, REGULAR GST, COMPOSITION
- Product GST rate persisted in `product_metadata`
- Existing products migrate with 0% GST
- Shared deterministic pricing rules for discount/tax calculations
- Checkout pricing preview uses the same pricing rules as transaction persistence
- Sale-level discount and tax totals persisted
- Sale-line taxable amount, discount, tax rate and tax amount persisted
- Historical sales preserve their original pricing snapshot
- Credit/Khata entry uses the final payable sale total
- Checkout remains offline/local-first
- GST is never automatically added when the store is NO_GST or COMPOSITION

## Transaction boundary

Room checkout performs pricing, stock deduction, inventory movement recording, sale persistence and credit-ledger persistence inside the existing transaction.

The transaction reads the store GST mode and product tax rate from local Room state at checkout time. The saved sale contains the resulting tax/discount snapshot so later changes to settings do not rewrite historical invoices.

## Current limitation

Bill-discount UI and price-override permissions are not yet exposed in the cashier workflow. The transaction layer supports a bill-discount amount for future UI integration, but the normal checkout currently passes zero.

Receipt rendering still needs to expose the persisted tax/discount breakdown.

GitHub Actions are intentionally not used during the current zero-minutes period.
