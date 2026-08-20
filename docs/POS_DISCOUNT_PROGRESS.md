# RetailPOS V2 — POS Discount Progress

## Implemented

- Checkout accepts a bill-level discount.
- Discount can be entered as a fixed INR amount or percentage.
- Percentage discounts are capped at 100%.
- Negative, non-finite and over-subtotal discounts are rejected by the pricing rules.
- Bill discounts are allocated proportionally across cart lines before product GST calculation.
- The checkout preview and Room transaction use the same discount amount.
- Sale-level and sale-line discount amounts are persisted.
- Credit/Khata sales use the final discounted/taxed total.

## Not yet implemented

- Staff role/permission enforcement for discounts.
- Maximum cashier discount policies.
- Item-level discount controls.
- Price overrides.
- Receipt rendering of the new persisted discount/GST breakdown still needs to be wired through the existing receipt formatter.

## Validation

Unit coverage includes no-tax pricing, product-specific GST, mixed-rate baskets, and proportional bill-discount allocation.

GitHub Actions are intentionally not used during the current Actions-minutes constraint.
