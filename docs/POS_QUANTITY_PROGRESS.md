# RetailPOS V2 — POS Quantity Progress

The POS quantity layer now uses the same validated `CartManager` rules for scanner/search/voice/manual quantity changes.

## Implemented

- Decimal/fractional cart quantities
- Direct quantity editing
- Increase/decrease controls
- Unit-aware adjustment steps
- Manual quantity entry with decimal/comma parsing
- Stock validation on every quantity change
- Quantity edits preserve the product's configured unit price
- Invalid, zero, negative and over-stock quantities are rejected
- Voice-created loose-item quantities and manual edits converge on the same cart state

## Unit adjustment defaults

- kg / litre: 0.05 units per +/- tap
- g / ml: 50 units per +/- tap
- pieces/other count-based units: 1 per +/- tap

Direct entry remains available for values such as `0.025 kg`, `250 g`, `0.375 litre`, or `3 pieces`.

## Safety

The UI does not mutate cart quantities directly. It calls the cart manager's validated quantity setter. Stock is re-read from the local Room product record before applying a manual edit.

GitHub Actions are intentionally not used for this development batch because the repository is under the current zero-minutes constraint.
