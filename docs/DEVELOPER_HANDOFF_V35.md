# RetailPOS V2 — Developer Handoff V35

Branch: `retailpos-v2`

## Audit checkpoint

This is the post-feature-freeze source audit. The audit was performed by inspecting the active branch, build configuration, Room database/migrations, POS transaction code, permissions, backup/restore, scanner, recovery flow, legacy activities and unit-test tree. GitHub Actions were not triggered.

## Real defects found and fixed

1. **Backup schema mismatch** — `DatabaseBackupManager` still labeled the database as schema 24 while `RetailDatabase` is version 25. Fixed to use schema 25.
2. **Backup parser resource-exhaustion risk** — encrypted/container parser accepted unbounded salt/IV/payload sizes. Added strict bounds and fixed-size salt/IV validation.
3. **Unsafe restore replacement** — restore deleted the active DB before guaranteeing the replacement could be activated. Restore now protects the old DB and rolls back when activation fails.
4. **Automatic Android backup privacy risk** — disabled unencrypted OS-level app backup because the POS database contains business/customer data and explicit backups are encrypted.
5. **Active-cart recovery overstated in documentation** — recovery validation existed but was not actually integrated into the POS shell. Added startup validation, blocking checkout while invalid saved lines remain, and a recovery dialog for removal/review.
6. **Held-bill price integrity bug** — resuming a held bill replaced the stored unit price with the current product selling price. Resume now preserves the stored price/override/discount snapshot.
7. **CI toolchain drift** — workflow used Gradle 9.3.1 while the project uses AGP 8.13.2. Aligned CI to Gradle 8.13 and JDK 17.

## Current technical state

- Room database: **v25**
- Migration 24→25: held-bill item-level price override + item discount
- Product barcodes: canonical `product_barcodes`
- Android automatic backup: disabled
- Explicit backup: encrypted AES-GCM with PBKDF2-derived key and checksums
- Active cart: durable local recovery with stale-state validation
- Product scanner: QR/payment/link-like payloads ignored before lookup
- Staff authentication: intentionally process-local; app restart requires staff authentication

## Important verification limitation

The source audit is not a substitute for compilation or physical-device verification. The project has not yet been run through the full build/test/device campaign after these fixes, and the repository still lacks the committed Gradle wrapper files (`gradlew`, `gradlew.bat`, wrapper JAR). Do not claim build-green until the real build is run.

## Next phase

1. Build in Colab with JDK 17 + Gradle 8.13.
2. Run unit tests and debug APK build.
3. Install APK on the spare Android via ADB.
4. Execute the complete integration/device acceptance matrix.
5. Fix every observed compiler/runtime/UI/device issue.
6. Only then consider deliberate CI verification when Actions budget is available.
