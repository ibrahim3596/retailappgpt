# RetailPOS V2 — Developer Handoff V32

Branch: `retailpos-v2`

## Development state

Core shopkeeper feature development is approximately 82–85% complete. Release readiness is materially lower because full build, device, migration and regression verification has intentionally been deferred until the feature set is frozen.

## Completed in this pass

- Added `BackupManifest` stable format contract.
- Added named `BackupSection` registry covering core retail data domains.
- Added checksum-protected `BackupPayload` envelope for future file export/import.
- Added `ActiveCartRecovery` coordinator with explicit missing, archived, insufficient-stock and invalid-pricing states.
- Added backup round-trip/tamper tests.
- Confirmed product QR safety already exists and added explicit regression tests.

## Important repository constraints

- Do not use GitHub Actions for debugging while the Actions budget is unavailable.
- Do not claim CI green without an actual successful workflow result for the exact commit.
- Preserve Room migration discipline.
- Keep `product_barcodes` authoritative; `ProductEntity.barcode` remains compatibility-only.
- Never treat arbitrary QR content as a product.
- Weak intelligent-capture suggestions must remain reviewable and must not silently become confirmed products.

## Known deferred work

- Integrate active-cart recovery coordinator into the POS startup UI so issues are shown immediately after process recovery.
- Integrate `ProductBarcodeSafety` directly into the physical scanner callback so ignored QR payloads never enter product lookup.
- Build the actual backup export/import file service around the `BackupPayload` envelope.
- Add backup encryption/password protection before exposing export/import to users.
- Preserve item-level pricing overrides in held bills through a proper Room migration. A tentative v25 migration was created and then removed because `RetailDatabase.kt` could not be safely updated through the GitHub contents API; do not leave the schema at a mismatched intermediate state.
- Continue offline transaction/recovery hardening.
- Final feature freeze followed by full local/physical-device QA.
