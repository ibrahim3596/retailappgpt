# Intelligent Product Capture — Batch Progress

## Completed in this batch

1. Product + capture metadata can now be saved through a Room transaction when an observation is supplied.
2. ProductReviewScreen passes the reviewed capture observation into product save.
3. Observed pack quantity is validated against the configured selling unit before save.
4. Visual category hints remain review-only and are not automatically persisted.
5. Confidence/evidence now includes text agreement, repeated-frame agreement and pack compatibility.
6. Multi-frame consensus logic was added with unit tests; the live camera still needs to feed multiple frames into the consensus layer.
7. Capture feedback remains bounded and never confirms a weak identity automatically.

## Safety boundary

- Barcode/catalog identity still requires retailer review.
- Pack size is observational metadata and does not overwrite the selling unit.
- Visual category hints do not become stored categories automatically.
- Price, purchase price, stock, SKU and tax remain retailer-controlled.

## Remaining Intelligent Capture work

- Feed live CameraX observations into the multi-frame consensus layer.
- Better OCR noise handling.
- Text-to-candidate normalization and candidate ranking across multiple candidates.
- Reliable MRP/price evidence extraction.
- Variant detection.
- Correction feedback persistence for future ranking.
- Offline common-product dataset.
- Final unknown-barcode → capture → save → return-to-bill integration audit.
