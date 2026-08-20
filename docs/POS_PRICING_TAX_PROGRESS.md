# RetailPOS V2 — POS Pricing & GST Progress

## Verified pricing foundation

`PricingRules` is now a pure business-rule layer with:

- `NO_TAX`
- `GST_ADDED`
- `GST_INCLUSIVE`
- discount-before-tax calculation
- non-negative/finite input validation
- tax-rate validation from 0–100%

The default is `NO_TAX` so the POS never invents a customer-facing tax charge.

## Store GST status

Settings now stores an explicit local GST status:

- I don't charge GST
- Regular GST taxpayer
- Composition taxpayer

The default is `I don't charge GST`.

For a composition taxpayer, RetailPOS must not show GST as an additional customer charge. This matches CBIC guidance that composition suppliers cannot collect tax separately from customers.

## Important implementation boundary

The current checkout persistence still stores only the existing sale totals. Tax and discount amounts are therefore **not yet persisted into completed sales**. Do not mark the full tax/discount checkout feature complete until:

1. product-level tax rates are persisted,
2. checkout calculates tax per applicable product,
3. sale and sale-line tax/discount amounts are persisted,
4. receipts show the correct tax treatment,
5. tests cover checkout persistence and historical totals.

No tax should be silently added before that integration is completed.

GitHub Actions were intentionally not used for this slice.
