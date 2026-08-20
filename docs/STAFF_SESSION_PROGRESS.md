# Staff Session Progress

## Implemented

- Room-backed `staff` accounts and migration 14 -> 15.
- Local username + PIN authentication.
- Salted PIN hashing and constant-time verification.
- Four-to-eight digit PIN policy.
- Five failed attempts trigger a five-minute local lockout.
- Inactive staff accounts are rejected.
- First-run owner setup flow.
- Mandatory staff access gate before the POS shell.
- Authenticated `StaffSession` propagated into the POS shell.
- Checkout receives the authenticated staff role.
- Bill-discount authorization is enforced inside the Room checkout transaction.
- Canonical `StaffRole` is shared by authentication and permission rules.

## Deliberate limitations

- Sessions are currently in-memory and require sign-in after process/app restart.
- No cloud identity provider is used or required.
- Staff management UI is not yet implemented.
- PIN reset/change workflow is not yet implemented.
- Automatic session timeout/lock is not yet implemented.
- Price overrides and item-level discount authorization are defined but not yet exposed through a complete cashier workflow.

## Validation

- PIN hashing/verification tests exist.
- PIN policy tests exist.
- Role permission tests exist.
- Checkout permission enforcement is performed in the transactional DAO.
- GitHub Actions were intentionally not triggered while Actions minutes are unavailable.
