# Inventory hardening progress

## Implemented in this slice

- Central `InventoryRules` for adjustment/receiving validation.
- Finite-value validation for stock quantities and purchase prices.
- Negative stock remains blocked transactionally by Room updates.
- Past expiry dates are rejected on receiving.
- Central stock states: healthy, low, out-of-stock, invalid.
- Inventory screen uses the central stock-state rules.
- Expiry policy defines a 30-day warning window and an explicit expired-for-sale rule.
- Existing checkout FEFO query excludes expired batches before allocation.
- Unit tests cover adjustment, receiving, stock-state and expiry-state rules.

## Not yet complete

- Dedicated expiry dashboard/alerts UI.
- Batch-specific return allocation.
- Inventory valuation and COGS.
- Persistent held-bill database migration.
- Full repository/device integration test pass.
