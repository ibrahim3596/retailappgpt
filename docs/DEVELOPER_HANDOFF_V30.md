# RetailPOS V2 — Developer Handoff V30

## Branch
`retailpos-v2`

## Current checkpoint
Active-cart recovery is now protected by a checkout preflight in `SaleDao.checkout`.

## Completed in this slice
- Restored/active cart lines are checked against the current product catalog before checkout.
- Archived products are rejected before any batch or stock mutation.
- Current stock is compared with the saved cart quantity before checkout.
- Item pricing/override permissions are revalidated at the transaction boundary.
- Invalid restored-cart pricing is rejected before checkout mutations.
- Existing Room transaction behavior remains atomic.
- No database schema change was required.

## Why this matters
The active bill is persisted locally and can survive process death. Product state can change while the app is closed, so the persisted bill must not be trusted blindly. The preflight prevents a stale bill from selling an archived, deleted, or under-stocked product and prevents partial inventory mutation before the error is shown.

## Still not claimed
- No CI verification was run.
- No full Gradle/device regression campaign was run.
- The complete feature freeze has not happened yet.

## Next development targets
1. Add explicit restored-cart user warning/recovery UI instead of waiting until checkout for every stale-state message.
2. Finish remaining offline reliability cases: process death during held-bill restore, app storage pressure, interrupted navigation, and duplicate user actions.
3. Final staff permission audit across every Activity and entry point.
4. Feature freeze and comprehensive local/device QA later.

## Hard constraints
Do not trigger GitHub Actions during the current zero-minute period. Do not claim CI-green without a successful CI result for the exact commit.
