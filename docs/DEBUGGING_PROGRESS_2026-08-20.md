# Debugging Progress — 2026-08-20

## Fixed in this pass

- Held-bill resume is claimed atomically through the Room transaction path rather than loading the bill into the cart before deleting it.
- Barcode scanner CameraX/ML Kit resources are lifecycle-owned and disposed with the screen instead of being recreated by `AndroidView.update`.
- Pending checkout recovery state is initialized through `RetailPosApplication`, allowing persisted cash/idempotency state to be loaded after process recreation.
- Shared pricing rules now round monetary outputs to two decimal places to prevent fractional-paise values from propagating through checkout.

## Validation

- Added a regression test for currency rounding.
- Performed cross-file source inspection of checkout, sale transaction, payment settlement, staff gate, inventory mutation and recovery paths.
- GitHub Actions were not triggered.
- A full Gradle/device build remains unverified in this environment.

## Remaining high-priority risks

- Settings screen still reads some business settings from SharedPreferences while checkout reads Room; this should be consolidated so restored data cannot drift.
- `MainActivity` still contains too much orchestration and should gradually delegate mutation workflows to use cases/repositories.
- Historical COGS/reporting needs an explicit return-cost adjustment model so returned stock does not remain counted as sold COGS.
- The old tracked `tmp/` verification markers should be removed as repository cleanup.
