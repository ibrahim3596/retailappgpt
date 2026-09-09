# RetailPOS V2 — Core 10/10 Milestone

Completed in this milestone:

- Room schema 25 with sale cost allocations, returns/return lines, suppliers/purchases and persisted favorites.
- Checkout snapshots actual FEFO/purchase-cost basis for new sales.
- COGS query for daily owner reporting.
- Transactional full/partial returns with return-quantity validation.
- Return permission enforcement through the existing staff permission matrix.
- Return stock restoration, with known original-batch restoration and proportional allocation for partial returns.
- Credit-sale return reverses the customer Khata entry.
- Returns/refunds activity and dashboard entry.
- Owner dashboard shows live daily sales, bill count, items, cash, UPI, card, credit, tracked COGS and gross profit.
- Home/Overview stock attention counts are now derived from live product inventory state.
- Home RetailGPT attention insight chooses a contextual operational action from local store data.
- Analytics uses return-adjusted net sales, net items, restored COGS, payment mix and top-product reporting.
- Day-end counted-cash variance is shown from expected cash.
- Purchase entry blocks duplicate product lines before Room persistence and records supplier payable/payment ledger entries transactionally.
- Return rules and payment/checkout foundations have dedicated unit tests.
- POS quick-add recently-sold and favorite products are loaded from local Room-backed store data.

Accounting boundary:

- Sales created before cost-allocation history was introduced have no historical cost allocations and therefore contribute zero to the tracked COGS query rather than receiving invented historical costs.
- Gross profit uses net sales and return-adjusted tracked COGS where return restoration data is available.
- External UPI/card provider confirmation is not claimed; those flows remain cashier-confirmed until a provider integration supplies a success/reference signal.

Validation boundary:

- The active UI branch is `retailpos-v2-ui-latest`.
- Latest successful Android CI validated KSP, Kotlin compilation, unit tests, lint, debug APK assembly and APK existence on commit `f5e16bb37168e75112fb0f6eac23be86f8e36d12`.
- Subsequent POS quick-add UI/documentation commits are being validated by the same Android CI workflow before they are treated as green.
