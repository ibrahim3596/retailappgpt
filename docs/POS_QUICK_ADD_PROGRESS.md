# POS Quick-Add Progress

## Implemented in this batch

- Deterministic recent-product deduplication and limit rules.
- Addable-product filtering; zero/invalid stock is never presented as addable.
- Process-local favorites with toggle semantics.
- Reusable Compose quick-add surface for recently sold/favorite products.
- Unit tests for deduplication, zero-stock filtering, favorites and recent-product ordering.

## Current boundary

The existing Room database currently remains version 17. The Room write path for the held-bill persistence migration has been unreliable through the repository contents endpoint, so favorites remain process-local and the new quick-add component is not yet connected to persistent store-scoped favorite data.

The next POS integration pass should connect the reusable quick-add surface to recent `SaleEntity`/`SaleLineEntity` data in `MainActivity`/`PosScreen`, then move favorites into Room once the database file can be updated safely.

No GitHub Actions were triggered.
