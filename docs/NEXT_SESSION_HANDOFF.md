# RetailPOS V2 — Next Session Engineering Handoff

## Session date
2026-08-21

## Repository state

- Repository: `ibrahim3596/retailappgpt`
- Active branch: `retailpos-v2`
- Latest verified commit at handoff creation: `b3dbcacadc483f8b780e031893a8cc96ce976c58`
- Latest commit message: `fix: clear transient billing state after database restore`
- Current Room schema: 25
- CI: DO NOT RUN GitHub Actions during the current Actions-budget restriction.

The repository is the source of truth. Re-read the current code before changing anything. Previous chat summaries may contain stale claims.

---

# 1. Product direction

RetailPOS V2 is a serious shopkeeper-first, offline-first Indian POS.

Core loops:

SELL
Product → Cart → Payment → Stock ↓

BUY
Supplier → Purchase → Receive → Stock ↑ → Payable ↑

COLLECT
Customer → Credit/Khata → Payment → Balance ↓

The flagship differentiator remains Intelligent Product Capture and voice-driven kirana billing.

---

# 2. Important business rules

## Loose goods

The POS must support decimal quantities:

- kg
- g
- litre/l
- ml
- pcs
- packet
- pack
- pouch
- bottle
- box
- tin
- jar
- sachet

Example:

`aadha kilo shakkar`

→ Sugar
→ 0.5 kg
→ if price is ₹50/kg: ₹25

Voice must never silently create an ambiguous product.

## Voice

Hindi is the baseline language.

Target Indian languages include:

- Hindi
- English India
- Telugu
- Malayalam
- Marathi
- Tamil
- Kannada
- Bengali
- Gujarati
- Punjabi
- Odia

Use Android recognizer capability detection. Do not assume every device has every language model.

Voice pipeline:

speech → structured quantity/unit/product query → local product lookup → validation → cart

Critical billing/pricing logic must remain deterministic and local.

## Barcode / QR

Canonical barcode model: `product_barcodes`.

`ProductEntity.barcode` is only a compatibility mirror.

Normal product scanning must reject arbitrary QR payloads.

Payment/business QR must remain separate.

## GST

Do not automatically add GST for every kirana shop.

Store modes:

- `NO_GST`
- `REGULAR`
- `COMPOSITION`

Regular GST uses persisted product tax rates.

Composition must not display GST as a separate customer charge.

Historical sales must retain their original tax/discount snapshot.

---

# 3. Major functionality already implemented

Verified foundations/current implementation include:

- Product master
- SKU validation
- primary/alternate barcodes
- barcode validation
- products without barcodes
- product metadata
- product images
- catalog lookup
- persistent identification cache
- OCR cleanup
- candidate ranking
- evidence/confidence model
- unknown barcode → intelligent capture → save → return to POS
- multilingual voice foundation
- multi-item voice parsing
- loose-item decimal billing
- fractional cart quantities
- favorites/recently sold foundations
- bill discounts
- GST modes and product tax rates
- transactional sale pricing snapshots
- payment validation
- cash/change persistence
- credit/Khata sales
- held bills
- active cart persistence
- checkout idempotency fingerprints
- encrypted database backup/restore
- inventory receiving/adjustment
- FEFO batch allocation
- returns/refunds
- return-aware headline analytics
- staff roles and permission foundations
- store-isolated return validation

Do not reimplement these blindly. Audit the exact current code first.

---

# 4. Important fixes completed in recent sessions

## Recovery

- Fixed `PendingPaymentStore` startup initialization.
- Fixed active-cart persistence after process death.
- Added cart-aware idempotency fingerprinting.
- Expanded transaction fingerprint to include checkout context.
- Cash recovery is tied to the cart fingerprint.
- Database restore now clears active cart and pending billing state so an old bill cannot be reopened against a restored database snapshot.

## Held bills

`HeldBillRepository.takeForResume()` validates and claims the bill inside one Room transaction.

Do not assume it is still racy without re-reading the code.

## Scanner

CameraX/ML Kit resources were moved away from recomposition-driven creation and toward lifecycle-owned cleanup.

Re-audit only if the current code contradicts this.

## Intelligent capture

Fixed premature `ImageProxy` closure and unsafe asynchronous evidence aggregation.

## Returns

Returns now enforce store ownership.

Analytics has return-aware headline calculations.

## Product integrity

Product + metadata validation now checks matching product/store identity.

Product + barcode/metadata writes use transactions where the repository has a database handle.

---

# 5. Remaining bugs / fixes to complete

Treat this as a prioritized backlog, not permission to stop after one item.

## P0 — data integrity / production safety

### P0.1 Full checkout integration verification

Need a real repository-level audit/tests for:

- cart → pricing → payment → sale → inventory → Khata
- duplicate checkout
- stale idempotency state
- process death during checkout
- stock changing between preview and transaction
- batch depletion during checkout
- expired-only batch stock
- partial failure rollback
- credit amount exactly equals final payable

The core transaction exists, but the integration test suite is incomplete.

### P0.2 Return/analytics consistency

Current ReturnDao has return summaries.

Analytics now needs/has partial return-aware treatment.

Continue verifying:

- net payment mix
- net top sellers
- returned sales in recent views where relevant
- refund method reconciliation
- cash/UPI/card balances after returns
- Khata return effects

Do not double-subtract returns.

### P0.3 Full transaction audit

Audit every Room `@Transaction` path involving:

- sale
- return
- purchase
- stock receive
- Khata settlement
- customer payment
- supplier payment
- held bill claim

Verify each has atomic failure behavior.

### P0.4 Referential integrity review

Audit relationships between:

- products ↔ metadata
- products ↔ barcodes
- sales ↔ sale lines
- sales ↔ cost allocations
- returns ↔ return lines
- purchases ↔ purchase lines
- customers ↔ ledger
- suppliers ↔ ledger/purchases
- batches ↔ products

Look for cross-store access and orphan-record risks.

### P0.5 Database migration verification

Room is version 25.

Audit every migration in `DatabaseMigrations.kt` / database configuration.

Add/expand migration tests for the entire chain.

Do not modify schema casually.

---

## P1 — billing correctness

### P1.1 Money precision

There is still substantial `Double` usage.

Current central pricing rounds monetary results, but the project still needs a full money/quantity precision policy.

Prefer a central money abstraction or fixed-scale strategy rather than scattered `Double` arithmetic.

Do not perform a giant rewrite; tackle pricing/payment/reporting first.

### P1.2 Payment completion

Current payment flows are primarily cashier-confirmed.

Audit:

- cash tendered/change
- UPI amount validation
- card amount validation
- split payment support
- credit payment/Khata
- accidental double completion
- screen recreation during checkout

### P1.3 Price overrides

Implement a production-safe permissioned price override workflow.

Requirements:

- role permission
- clear audit reason
- original price preserved in sale snapshot
- no silent override
- applies before/within pricing engine consistently

### P1.4 Item-level discounts

Implement only if consistent with existing pricing engine.

Must not double-apply with bill discount.

---

## P1 — inventory

### P1.5 Held-bill vs stock race UX

The held-bill claim itself is atomic.

However, stock can change after a bill is resumed and before checkout.

Checkout already revalidates stock.

Improve the UI so stale stock on resumed bills is explicit and easy to fix without losing the whole bill.

### P1.6 Expiry handling

Audit:

- expired batch never sold
- near-expiry warning UI
- return of expired merchandise
- purchase/receive expiry rules
- reports based on actual batch state

### P1.7 Inventory valuation / COGS

Current batch cost allocation exists.

Complete:

- valuation UI
- COGS reconciliation
- return-adjusted COGS
- purchase cost changes
- fallback purchase cost behavior

---

## P1 — returns

The roadmap is stale about returns; the core implementation already exists.

Remaining work:

- find previous sale UX
- partial/full return UX
- refund audit history
- payment-method-specific reconciliation
- receipt/return note
- permissions UI
- return reporting

---

## P1 — POS UX

Current POS is functionally good but visually/interaction-wise not release-polished.

Current PosScreen strengths:

- clear BILLING header
- voice button
- scanner access
- recently sold
- favorites
- cart
- quantity editing
- hold/resume/clear
- prominent checkout
- recovery warning

Current weaknesses:

- scanner affordance is duplicated in the top bar and search row
- HOLD / RESUME / CLEAR consume a lot of horizontal space
- quantity editing is somewhat dense
- search results are still fairly generic cards
- no truly optimized two-thumb/one-hand billing layout
- voice does not yet have a refined review/retry UX
- no polished quick-add grid for common products

Next UX direction:

- larger product quick-add tiles
- stronger numeric quantity input for loose goods
- make scanner one obvious primary action
- keep voice one obvious primary action
- reduce duplicate controls
- make checkout visually dominant
- optimize for fast repeated billing

Do not redesign the whole app blindly.

---

## P1 — Home/dashboard UX

Current HomeScreen is functional but text-heavy.

Strengths:

- Today summary
- New Bill primary CTA
- purchase/receive shortcut
- returns
- expenses
- cash reconciliation
- permission-aware quick access

Weaknesses:

- too many vertically stacked full-width actions
- dashboard has a lot of text for a shopkeeper-first home screen
- quick access could become a compact icon grid
- key business metrics could be organized visually
- cashier should see a fast billing-first home
- owner/manager should see more business metrics

Recommended direction:

Cashier home:

1. New Bill
2. Scan/Voice
3. Recent bills/held bills
4. Customers/Khata
5. compact quick actions

Owner/manager home:

1. Today sales
2. cash/UPI/card/credit
3. gross profit/COGS
4. stock/expiry alerts
5. quick operational actions

Do not introduce role-specific dashboards until business permissions are correctly enforced.

---

## P1 — documentation/repository cleanup

### P1.8 Remove `tmp/` marker files

The repository still contains many historical verification/CI marker files under `tmp/`.

They are clutter and should be removed as one coherent cleanup batch after enumerating the entire directory.

Do not delete legitimate product assets without checking.

### P1.9 Synchronize handoff documentation

`docs/DEVELOPER_HANDOFF.md` contains stale historical claims, especially older Room version information.

The actual code is much newer.

Update the canonical handoff to reflect current reality.

Do not rewrite away historical context that is still useful; clearly label current state vs history.

### P1.10 Roadmap synchronization

`docs/ROADMAP.md` is more current than the handoff but still contains items marked incomplete that are actually implemented at a partial/end-to-end level.

Audit each checkbox against the code before changing it.

---

## P2 — product / intelligent capture

### P2.1 Authoritative unit inference

Improve capture so observed `500 g`, `1 kg`, `1 litre`, etc. can inform the candidate without confusing pack-size with selling unit.

### P2.2 Category inference

Suggestion only unless deterministic evidence is strong.

### P2.3 Catalog freshness/source policy

Add cache freshness and source priority.

### P2.4 Dedicated image recognition service

Keep it optional and review-first.

### P2.5 Offline common-product dataset

Future feature, not an excuse to introduce online dependency into core billing.

### P2.6 Device capture hardening

Audit:

- permission denial
- camera retry
- low light
- rapid scans
- lifecycle recreation
- analyzer backpressure
- repeated callback prevention

---

## P2 — Staff/security

Remaining:

- price override permission
- item discount permission workflow
- refund permission UI/enforcement audit
- report permission UI
- settings permission UI
- staff activity audit
- session timeout/lock
- broader authorization boundary audit

Never rely only on hidden UI buttons.

Business operations must enforce authorization.

---

## P2 — reporting

Need a proper owner dashboard eventually:

- today sales
- date ranges
- average bill
- payment mix
- discounts
- GST
- returns
- best sellers
- slow movers
- gross profit
- COGS
- inventory valuation
- expiry
- Khata
- supplier payables
- purchases
- expenses
- exports

---

## P3 — suppliers / purchasing

Current persistence foundations exist.

Next coherent feature slice:

Supplier CRUD
→ purchase entry
→ receive stock
→ batch/expiry
→ payable
→ supplier payment
→ purchase history

---

# 6. Current UI assessment

## Overall

**Functional quality:** good foundation.

**Production visual polish:** not there yet.

**Shopkeeper speed:** promising, especially POS.

## POS

Best current screen.

The layout already emphasizes:

- billing title
- voice
- scanner
- quick add
- cart
- quantity controls
- checkout

But it still needs a deliberate speed pass.

## Home

Useful dashboard, but too vertically stacked and text-heavy.

It currently reads more like an admin dashboard than a fast retail terminal.

## Settings/product/inventory

Need a later design-system pass for consistency:

- same button hierarchy
- same field sizes
- same top-bar treatment
- consistent error/loading/empty states
- consistent card spacing
- standard touch targets

Do not redesign before the major functional bugs are cleared.

---

# 7. What NOT to do next

Do not:

- run GitHub Actions repeatedly
- create dummy trigger commits
- rewrite all architecture
- replace Room with another database
- introduce cloud dependency into billing
- let LLMs decide prices
- let weak AI guesses silently create products
- make arbitrary QR content a product
- hardcode GST assumptions
- destroy retailer-controlled prices/stock/SKU during catalog recognition
- claim CI-green without actual CI

---

# 8. Recommended next large chunks

## Chunk A — repository hardening

1. Remove `tmp/` markers.
2. Synchronize handoff docs.
3. Audit roadmap checkboxes.
4. Full migration chain review.
5. Referential integrity audit.

## Chunk B — transaction/integration hardening

1. Full checkout integration tests.
2. Returns/refunds integration tests.
3. Purchase/receive/payable integration tests.
4. Khata/payment integration tests.
5. Process-death recovery tests.
6. Idempotency regression tests.

## Chunk C — money/inventory hardening

1. Central money precision strategy.
2. COGS/valuation reconciliation.
3. Expiry behavior.
4. Held-bill stale-stock UX.
5. Inventory recovery.

## Chunk D — POS completion

1. Price override permission.
2. Item discount permission.
3. Split payments.
4. Receipt/return receipt polish.
5. Faster quick-add workflow.
6. Voice review/retry UX.

## Chunk E — UI polish

1. Home redesign for cashier/owner roles.
2. POS one-handed/touch-speed pass.
3. Design tokens/components.
4. Loading/error/empty states.
5. Accessibility/touch target audit.

## Chunk F — Intelligent Capture completion

1. Unit inference.
2. Category inference.
3. Catalog freshness.
4. Device capture hardening.
5. Image recognition service.
6. Offline common-product dataset.

After these, continue suppliers/purchasing and broader reporting.

---

# 9. Definition of done

A feature is done only when:

1. UI exists.
2. Business rules exist.
3. Persistence exists where needed.
4. Navigation/back behavior works.
5. Loading/error/empty states are handled.
6. Edge cases are handled.
7. Adjacent workflows are integrated.
8. Tests exist.
9. Offline behavior is considered.
10. Documentation is updated.
11. CI is deliberately verified later when Actions budget is available.

---

# 10. Startup instructions for next session

Before editing:

1. Confirm branch.
2. Read `README.md`.
3. Read `docs/ROADMAP.md`.
4. Read `docs/DEVELOPER_HANDOFF.md`.
5. Read this file.
6. Inspect latest commits.
7. Compare documentation to actual source.
8. Re-check current Room version.
9. Start with Chunk A/P0 issues.
10. Do not ask the user to paste repository files.

Then continue fixing in large coherent batches.
