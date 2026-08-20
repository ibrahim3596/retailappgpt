# RetailPOS V2 — Developer Handoff V31

## Branch
- `retailpos-v2`

## Development checkpoint
- Offline/recovery hardening batch completed.
- No GitHub Actions used.
- Full compile/device verification remains deferred until feature development is frozen.

## Completed in V31 batch
1. Added pure active-cart recovery validation for missing, archived, insufficient-stock and invalid-pricing cases.
2. Added recovery unit coverage.
3. Added normalized receipt payment summary support for split tenders.
4. Integrated split payment components into receipt output.
5. Added a backup manifest model for local/offline backup metadata.
6. Added explicit backup section scope definitions.
7. Added barcode/QR safety classifier to prevent arbitrary payment URLs/vCard/URL-like payloads from becoming products.
8. Added deterministic inventory expiry status rules.
9. Added deterministic day-end cash reconciliation rules.
10. Added consolidated offline safety tests plus active-cart recovery messaging.

## Important deferred item
A proposed Room v25 migration for preserving pricing overrides inside held bills was deliberately reverted because the live `RetailDatabase.kt` contents endpoint was returning a conflicting SHA during the update. The stable Room schema remains version 24; no partial migration/entity change was left behind.

## Next work
- Integrate active-cart recovery evaluation on app start and display unsafe-line warnings immediately.
- Integrate barcode safety classification into scanner flow.
- Build actual backup/export file generation from `BackupScope` and `BackupManifest`.
- Continue offline reliability and process-death hardening.
- Then feature-freeze and perform the complete Gradle/device/physical-phone test campaign.
