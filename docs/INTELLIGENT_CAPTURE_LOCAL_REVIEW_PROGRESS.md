# Intelligent Capture — Local Candidate Review

## Implemented

- OCR/product-name normalization feeds the retailer's local product catalog before public catalog lookup.
- Local candidates are ranked using name/brand agreement and exact-name bonuses.
- Ranked local candidates are shown in `ProductReviewScreen` ahead of public catalog suggestions.
- The review action for a local match is `OPEN EXISTING PRODUCT`, preventing accidental duplicate-product creation.
- The existing product ID can be passed through `onExistingProductSelected` for navigation back to the product master.
- Local-candidate evidence can raise the visible identification confidence, but it does not silently confirm identity.
- Existing product pricing, stock, SKU and other retailer-controlled fields remain untouched.

## Remaining

- Persist acceptance/rejection feedback into a durable correction history.
- Add variant-aware local matching (size/flavour/pack variant).
- Add barcode-specific local candidate ranking before name matching.
- Complete the unknown-barcode billing flow audit end-to-end.
