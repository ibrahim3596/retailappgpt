# RetailPOS V2 — Core 10/10 Milestone

Completed in this milestone:

- Room v20: sale cost allocations + returns/return lines.
- Checkout snapshots actual FEFO/purchase-cost basis for new sales.
- COGS query for daily owner reporting.
- Transactional full/partial returns with return-quantity validation.
- Return permission enforcement through the existing staff permission matrix.
- Return stock restoration, with known original-batch restoration and proportional allocation for partial returns.
- Credit-sale return reverses the customer Khata entry.
- Returns/refunds activity and dashboard entry.
- Owner dashboard now shows live daily sales, bill count, items, cash, UPI, card, credit, tracked COGS and gross profit.
- Day-end counted-cash variance is shown from expected cash.
- Purchase entry blocks duplicate product lines before Room persistence.
- Return rules have dedicated unit tests.

Accounting boundary:

- Sales created before v20 have no historical cost allocations and therefore contribute zero to the tracked COGS query rather than receiving invented historical costs.
- Gross profit is currently sales minus tracked COGS. Return-adjusted gross profit and return-adjusted payment summaries remain the next reporting refinement.

CI boundary:

- No GitHub Actions were triggered for this milestone.
- CI/build-green status has not been claimed.
