# Intelligent Product Capture — Feedback, Variants, and Billing Handoff

## Completed

- Added Room `product_identification_feedback` storage with migration 16 -> 17.
- Persisted bounded retailer review signals for local candidates and public catalog candidates.
- Local candidate ranking now uses persisted feedback boosts within a bounded range.
- Capture ranking now compares observed pack size/unit against product metadata and favors exact pack variants.
- Conflicting feedback creates no learning signal.
- Unknown barcode -> product review now returns the actual saved product ID to the active bill.
- If a reviewer selects an existing local product, the flow can return that product directly to the active bill instead of creating a duplicate.
- Capture review passes observed pack size/unit into local candidate ranking.

## Safety boundaries

- Feedback does not store raw camera frames.
- Weak visual hints remain suggestions and are not persisted as authoritative product identity.
- Retailer-controlled price, purchase price, stock, SKU and selling unit remain authoritative.
- Feedback influence is bounded and cannot override strong current evidence.
- Barcode resolution remains authoritative through `product_barcodes`.

## Remaining

- Broader variant recognition across flavor/size/pack-family attributes.
- Richer feedback analytics and correction summaries.
- Full end-to-end test on a physical device for unknown barcode -> review -> save -> active bill.
