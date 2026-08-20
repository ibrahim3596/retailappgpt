# Intelligent Capture — Multi-frame Progress

## Completed

- Live CameraX analysis now feeds repeated capture observations into `ProductCaptureConsensus`.
- The analyzer keeps a bounded six-observation window.
- Barcode, OCR, visual label, MRP and pack evidence are merged conservatively.
- Capture analysis uses a lifecycle-scoped executor and releases ML Kit clients when leaving the screen.
- ImageProxy closure remains guarded so asynchronous ML Kit completion does not leak camera frames.
- Capture stability policy distinguishes barcode-backed review from text-only and visual-only evidence.
- Text-only identity requires repeated evidence before the capture action is enabled.
- Visual category hints alone never become stable product identity.
- Evidence-frame count and stability explanation are visible to the retailer.
- Existing product + metadata persistence remains atomic when a reviewed capture is saved.

## Deliberate boundaries

- The system still requires human review before product creation.
- Multi-frame consensus does not silently choose a product from visual evidence alone.
- Catalog identity does not override retailer-controlled selling price, purchase price, stock or SKU.
- Public catalog lookup remains a fallback source, not the authoritative product master.

## Next slice

- OCR normalization for common packaging print noise.
- Better candidate generation/ranking across local product, barcode, catalog and text evidence.
- Persisted correction/feedback signals.
- Unknown-barcode billing flow end-to-end audit.
- Camera retry/low-light guidance.
