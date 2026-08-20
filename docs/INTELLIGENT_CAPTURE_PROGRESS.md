# Intelligent Product Capture — Progress Audit

Updated during the `retailpos-v2` continuation work on 2026-08-20.

## Verified in code

- Persistent barcode-backed identification cache is configured through `RetailDatabase` and `ProductCatalogLookup`.
- Public catalog results are persisted in Room and can be reused without a network request.
- In-memory catalog caching is now positive-value-only because `ConcurrentHashMap` cannot store null values.
- OCR parsing is separated from the Compose screen and filters common metadata/noise lines before selecting product text.
- MRP extraction is isolated and rejects malformed price text instead of inventing a numeric value.
- Evidence-based candidate scoring now distinguishes barcode/catalog agreement from weaker OCR/visual evidence.
- Product review displays the confidence score and a human-readable explanation.
- Catalog candidates remain explicitly reviewable through `USE CATALOG` or `KEEP CAMERA`.
- Retailer-controlled price, purchase price, stock and SKU are not populated by catalog application.
- ML Kit image frames are kept alive until barcode, OCR and image-label analysis completes, preventing premature `ImageProxy.close()`.
- Standard product barcode scanning continues to exclude QR codes.
- Unknown-barcode billing flow already routes to intelligent capture or manual product creation and returns to the active POS when the original unknown barcode is preserved.

## Remaining Intelligent Capture work

- Multiple-frame capture and temporal evidence aggregation.
- Catalog source priority and freshness policy.
- Candidate ranking across multiple distinct catalog candidates rather than a single barcode lookup result.
- Pack-size/unit/category field inference into product metadata.
- Image-assisted recognition service beyond generic ML Kit image labels.
- Human correction/feedback persistence.
- Camera retry/restart and low-light guidance.
- Full device/manual validation of scan → identify → review → save → return-to-POS.

## Validation constraint

No GitHub Actions workflow was triggered during this work. CI/build-green status is intentionally not claimed until Actions minutes are available for deliberate verification.
