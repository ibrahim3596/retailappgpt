# Staff Gate Progress

## Implemented

- Staff accounts are persisted locally in Room.
- First launch requires creation of a local owner account.
- Subsequent launches require staff username/PIN authentication.
- PINs are salted and hashed locally.
- Failed PIN attempts lock an account for five minutes after five failures.
- `StaffSession` is held in process memory only.
- `StaffGateActivity` is the Android launcher activity.
- `MainActivity` is no longer exported and is launched only after successful staff authentication.
- Checkout uses the authenticated session role when no explicit staff role is supplied.
- Bill-discount limits are enforced in `SaleDao` before sale/stock persistence.

## Deliberate boundaries

- Session state is not persisted across process death.
- Staff management UI exists but still needs a navigation entry from the main settings flow.
- Cashier switching and explicit sign-out are the next session-management slice.
- No cloud authentication is required; the feature remains offline-first.

## Validation

Unit coverage exists for PIN hashing/verification, lockout policy, permission limits, and session lifecycle.

GitHub Actions were not used for validation while the Actions budget is unavailable.
