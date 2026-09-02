# POS Quick-Add Progress

## Current implementation

- Deterministic recent-product deduplication and limit rules.
- Addable-product filtering; zero/invalid stock is never presented as addable.
- Store-scoped favorites persisted in Room via `favorite_products`.
- Reusable Compose quick-add surface for recently sold/favorite products.
- Current POS loads recently sold products and Room-backed favorites from the local store.
- Quick-add UI uses larger operational tiles with clear price hierarchy and accessible add/favorite semantics.
- Unit tests cover deduplication, zero-stock filtering, favorites and recent-product ordering.

## Current boundary

- The quick-add surface remains horizontally scrollable rather than a dense fixed grid to preserve mobile cashier ergonomics.
- POS still has a duplicated scanner affordance in the top bar and search field; this should be consolidated in a later focused UX pass.
- Voice input has deterministic parsing and safe no-partial-add behavior, but its review/retry UX can still be refined.

## Data / platform state

- Room schema is currently version 25.
- Favorites are store-scoped and persisted locally.
- Existing checkout, held-bill, cart-recovery and payment logic should be preserved rather than reimplemented during UI refinement.
