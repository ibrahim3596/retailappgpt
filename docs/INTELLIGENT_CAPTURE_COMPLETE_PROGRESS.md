# Intelligent Product Capture — Completion Progress

## Implemented

- CameraX + ML Kit barcode/OCR/visual evidence pipeline.
- Live multi-frame consensus over the latest six observations.
- QR codes excluded from product identity.
- Lifecycle-safe analyzer/executor cleanup and main-thread UI state handoff.
- OCR normalization and explicit `Brand:`, `Product:`, and MRP extraction patterns.
- Pack-size extraction for weight, volume and package/count units.
- Package-vs-selling-unit compatibility checks.
- Local Product Master candidate generation before public catalog fallback.
- Exact local barcode candidate precedence.
- Local candidate ranking with name, brand, pack-variant and bounded historical-feedback evidence.
- Local/catalog conflict resolution rules.
- Public catalog lookup with canonical barcode normalization and Room-backed offline reuse.
- Review-before-save with retailer-controlled SKU, stock, purchase price and selling price preserved.
- Product + metadata + primary barcode atomic save.
- Durable correction feedback in Room with bounded ranking signals.
- Unknown-barcode billing flow returns the exact saved product ID to the active bill.
- Existing local candidates open the existing product instead of creating duplicates.

## Deliberately not claimed complete

- Authoritative automatic category inference.
- Automatic selling-unit inference.
- Dedicated image-assisted recognition service.
- Offline common-product dataset.
- Catalog freshness/versioning.
- Physical-device and low-light validation.
- Full integration/device test pass.

Those remain release-hardening or future intelligence work and must not silently promote weak visual evidence to confirmed product identity.
