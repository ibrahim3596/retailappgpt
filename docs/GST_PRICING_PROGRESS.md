# RetailPOS V2 — GST / Pricing Progress

## Implemented

- Deterministic pricing engine with `NO_TAX`, `GST_ADDED`, and `GST_INCLUSIVE` treatment.
- Discount is applied before tax calculation.
- Store GST mode is configurable locally: no GST, regular GST taxpayer, or composition taxpayer.
- Product metadata now stores `taxRatePercent`, defaulting to 0.0% for existing and new products.
- Room migration `11 -> 12` adds the tax-rate column without changing existing product prices.
- Product metadata editor can set a GST rate from 0% to 100%.
- Unit tests cover pricing and tax-rate validation.

## Deliberate boundary

Checkout has not yet been changed to charge or persist GST from this setting. The current sales schema still stores the legacy subtotal/total model. The next pricing slice must persist the exact tax and discount values used at checkout so historical receipts remain stable even if product/store settings change later.

## Safety

- Default tax rate is 0%.
- Composition mode does not represent GST as a separate customer charge.
- Invalid tax rates are rejected.
- Existing products migrate with 0% tax configured.
- GitHub Actions are intentionally not used during the current zero-minutes period.
