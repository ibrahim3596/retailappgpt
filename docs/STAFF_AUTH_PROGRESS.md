# RetailPOS V2 — Staff Authentication Progress

## Implemented

- Offline-first staff persistence in Room.
- Unique username per store.
- Salted PIN hashing using SHA-256 and per-account random salt.
- PIN validation: 4–8 numeric digits.
- Constant-time digest comparison during verification.
- Failed-PIN tracking.
- Account lockout after 5 failed attempts for 5 minutes.
- Inactive-account rejection.
- Staff roles use the canonical `core.permissions.StaffRole` model.
- Existing centralized permission rules remain the authority for discount/price permissions.
- Process-local staff session holder; PINs are never stored in the session.
- First-run owner setup/login UI foundation.
- Room migration 14 → 15 creates the staff table and username uniqueness index.

## Important current boundary

The new login/setup screen is implemented but is not yet the mandatory app entry gate. The existing single-user navigation still starts at Home. Until the gate is wired, the legacy owner/default flow remains available.

The next integration step is to make `StaffAccessScreen` the entry gate, create the first owner when no staff exists, store the authenticated session in `StaffSessionManager`, and pass the authenticated canonical `StaffRole` into checkout.

## Security notes

This is local-device authentication, not account recovery or cloud identity. A later cloud/multi-device system must not reuse these local PIN hashes as a network authentication protocol.

## Validation

Unit tests cover PIN hashing/verification, invalid PINs, canonical role permissions, and owner/staff management permissions. GitHub Actions are intentionally not used during the current Actions-minute restriction.
