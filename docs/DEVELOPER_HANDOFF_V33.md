# RetailPOS V2 — Developer Handoff V33

Branch: `retailpos-v2`

## Development state

Core shopkeeper feature development remains approximately 84–86% complete. Release readiness is materially lower because full build, device, migration and regression verification is intentionally deferred until feature freeze.

## Completed in this pass

- Integrated active-cart recovery warnings directly into POS.
- Disabled checkout while a recovered bill contains missing, archived, insufficient-stock or invalid-pricing lines.
- Integrated `ProductBarcodeSafety` into the CameraX/ML Kit scanner callback; invalid and QR/payment/link-like payloads are ignored before product lookup.
- Added encrypted AES-GCM backup container with PBKDF2 password derivation.
- Added consistent SQLite snapshot export using `VACUUM INTO`.
- Added checksum, schema-version and store-id validation during restore.
- Added controlled `RetailDatabase.closeForRestore()` for safe database replacement.
- Added Settings UI for encrypted backup export/import.
- Added unit tests for encryption round-trip, wrong password, corruption and active-cart recovery.

## Room / migration status

Database remains Room version 24. No held-bill pricing migration was added; held bills remain schema-compatible with the existing v24 database.

## Constraints

- Do not use GitHub Actions for debugging while the Actions budget is unavailable.
- Do not claim CI green without an actual successful workflow result for the exact commit.
- Preserve Room migration discipline.
- `product_barcodes` is authoritative; `ProductEntity.barcode` is compatibility-only.
- Arbitrary QR payloads must never become products.
- Intelligent capture suggestions remain reviewable and cannot silently become confirmed products.

## Remaining major work

- Full physical-device build/test and regression campaign after feature freeze.
- Backup import UX should require an explicit post-restore application restart before any further database interaction; current Settings messaging instructs the user to restart.
- Consider migrating held-bill item-level pricing only when a full Room migration can be applied coherently.
- Finish remaining offline transaction/recovery edge cases.
- Release hardening: performance, accessibility, crash recovery, export validation, printer/receipt hardware, permissions and security review.
- Cloud backup/sync, multi-device and multi-store remain future platform work.
