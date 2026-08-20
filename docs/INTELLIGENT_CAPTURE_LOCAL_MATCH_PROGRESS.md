# Intelligent Product Capture — Local Matching Progress

## Implemented

- OCR text normalization for local matching.
- Conservative local candidate DAO query by product name/brand.
- Local candidate ranking with name/brand similarity.
- Exact-name and strong-name evidence are scored separately.
- Weak matches are discarded rather than surfaced as confident candidates.
- Local matching remains offline and uses the retailer's own product master.
- Existing retailer-controlled price, stock, SKU and selling-unit fields remain authoritative.
- Unit tests cover normalization, exact-match ranking and weak-match rejection.

## Deliberate boundary

The ProductReviewScreen still needs the targeted UI integration that displays the ranked local candidates and lets the retailer explicitly accept/reject one before the public catalog lookup is applied.

Do not treat local candidate ranking as confirmed identity. It is another evidence source in the Intelligent Capture pipeline.

## Next

1. Show ranked local candidates in ProductReviewScreen.
2. Compare local candidate text with OCR evidence.
3. Feed local agreement into the central confidence score.
4. Persist bounded correction feedback for future ranking.
5. Audit unknown-barcode -> capture -> review -> save -> return-to-POS end-to-end behavior.
