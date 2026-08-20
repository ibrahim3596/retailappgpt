# RetailPOS V2 — Authoritative Developer Handoff (v20)

**Last refreshed:** 2026-08-20  
**Repository:** `ibrahim3596/retailappgpt`  
**Branch:** `retailpos-v2`  
**Latest verified branch head at handoff creation:** `b35e0f36057507982954391f19f59eed4471efdc`  
**Room version:** 20

> This file supersedes the older `docs/DEVELOPER_HANDOFF.md` for current development context. The repository is always the source of truth.

---

## 1. Current status

Feature development is currently **PAUSED for stabilization/testing**.

The app has been developed into a substantial kirana/general-trade POS foundation, but it has **not yet been proven green by a local Android Gradle build or physical-device test**.

The immediate task is:

```text
Windows setup
→ local Gradle build
→ unit tests
→ debug APK
→ install on spare Android
→ physical acceptance testing
→ logcat/debug
→ fix
→ rebuild/retest
→ only then resume feature development
```

GitHub Actions must NOT be used as the debugging loop.

---

## 2. Product strategy: three retail loops

The product is being shaped around:

```text
SELL
Product → Cart → Payment → Stock ↓

BUY
Supplier → Purchase → Receive → Stock ↑ → Supplier payable ↑

COLLECT
Customer → Credit sale → Khata ↑ → Payment → Khata ↓
```

The owner eventually needs:

```text
Sales + Cash/UPI + Inventory + Receivables + Supplier Payables + Margin
```

The app should remain a serious shopkeeper-first, offline-first Android POS, not an ERP and not an AI demo.

---

## 3. Technology / architecture

- Kotlin
- Android
- Jetpack Compose
- Room
- CameraX
- Google ML Kit

Preferred:

```text
Compose UI
↓
ViewModel/UI state
↓
Business rules/use cases
↓
Repository
↓
Room/network/device services
```

Do not perform giant architecture rewrites for aesthetics. Refactor incrementally around real feature boundaries.

---

## 4. Database v20

Room is currently **version 20**.

Migration chain is sequential through `19→20`.

Current important tables/entities:

- products
- product_barcodes
- product_metadata
- sales
- sale_lines
- inventory_movements
- inventory_batches
- customers
- customer_ledger
- product_identification_cache
- store_settings
- staff
- product_identification_feedback
- held_bills
- held_bill_lines
- suppliers
- purchases
- purchase_lines
- supplier_ledger
- sale_cost_allocations
- returns
- return_lines

`RetailDatabase` includes the v20 entities and registers `DatabaseMigrationsV20.MIGRATION_19_20`.

Never modify Room entities without a correct migration.

---

## 5. Product identity / barcode rules

Keep separate:

- retailer SKU
- primary barcode
- alternate barcodes
- barcode type
- GTIN/EAN/UPC-style identifiers
- no-barcode products

`product_barcodes` is canonical. `ProductEntity.barcode` is only a compatibility mirror.

Normal product flow:

```text
scan → normalize → validate → local canonical lookup → catalog fallback → intelligent identification
```

Arbitrary QR payloads must NOT become product identifiers. Payment/business QR remains separate.

---

## 6. Intelligent Product Capture — substantial foundation

Current architecture:

```text
Camera
↓
barcode + OCR + package/visual evidence
↓
multi-frame consensus
↓
normalized observation
↓
local Product Master candidate generation
↓
pack/variant matching
↓
historical bounded feedback
↓
public catalog/cache
↓
conflict/confidence evaluation
↓
human review
↓
atomic product + metadata save
↓
optional active-bill handoff
```

Implemented foundations include:

- live CameraX analyzer
- OCR
- barcode evidence
- multi-frame consensus
- OCR normalization
- brand/product extraction
- MRP extraction
- pack-size/pack-unit extraction
- local product candidate search
- exact-name preference
- pack/variant ranking
- persistent feedback
- local/catalog conflict resolution
- persistent catalog cache
- confidence/evidence UI
- unknown barcode → identify/add manually
- actual saved product ID returned to active bill
- retailer-controlled values preserved

Safety rules:

- weak visual guesses never silently confirm
- category visual hints remain suggestions unless reviewed
- packaging quantity must not overwrite selling unit
- catalog data must not silently overwrite retailer price, stock, SKU or tax settings

Remaining advanced capture work:

- authoritative category inference
- safe selling-unit inference
- image-assisted recognition service
- offline common-product dataset
- catalog freshness/versioning
- stronger low-light/retry/device hardening

Do not prioritize advanced vision over core retail reliability.

---

## 7. Current POS / SELL loop

Implemented foundations include:

- product search
- barcode billing scanner
- unknown barcode dialog
- voice billing
- decimal/loose quantities
- cart
- quantity editing/removal
- bill discount rules
- tax treatment foundation
- cash/UPI/card/credit
- amount tendered/change
- checkout idempotency
- atomic checkout transaction
- FEFO batch allocation
- inventory deduction
- customer credit ledger entry
- receipt/share flow
- persistent held bills

Voice target example:

`aadha kilo shakkar`

must become deterministic:

```text
product = sugar
quantity = 0.5
unit = kg
```

Speech parsing may understand language; deterministic business rules calculate quantity/unit conversion/stock/price.

Indian language plan: Hindi baseline plus Telugu, Malayalam, Marathi, Tamil, Kannada, Bengali, Gujarati, Punjabi, English/Hinglish and other downloadable language packs later.

---

## 8. BUY loop — now persistent

Implemented:

- supplier entity/DAO
- supplier creation/selection
- purchase entry UI
- purchase history
- supplier payable balance
- supplier payment
- purchase entity/lines
- multiple purchase lines
- paid quantity
- free quantity
- scheme discount
- effective purchase cost
- batch/expiry
- purchase repository transaction
- stock increase
- inventory batch creation
- inventory movement
- supplier payable ledger

Scheme rule:

```text
10 paid × ₹100 + 1 free
stock received = 11
paid cost = ₹1000
supplier payable = ₹1000
effective cost = ₹90.91/unit
```

Free stock does not increase supplier payable.

Purchase UI prevents duplicate products within a purchase because `(purchaseId, productId)` is the purchase-line key.

---

## 9. COLLECT loop — Customers + Khata

Implemented:

- customer list/search
- add customer
- duplicate phone protection
- Khata balance
- credit-sale ledger
- partial payment
- full settlement
- overpayment rejection
- statement/share
- deletion blocked with outstanding balance

Customer and supplier ledgers are separate concepts.

---

## 10. Inventory

Implemented foundations:

- stock adjustment
- stock receive
- inventory movement history
- batches
- expiry
- low-stock state
- out-of-stock state
- invalid-stock state
- FEFO selection
- expired-batch exclusion
- purchase-based receiving
- negative-stock protection

Central inventory rules validate finite values, quantities, purchase rates and expiry/batch combinations.

---

## 11. COGS / accounting foundation

Room v20 added:

`sale_cost_allocations`

Checkout records actual consumed cost from FEFO batches when available.

This supports:

- daily COGS
- gross profit
- historical cost evidence for post-v20 sales

Do not invent exact historical COGS for sales before the allocation feature existed.

---

## 12. Returns / refunds

Implemented at data/UI level:

- recent sale selection
- line quantities
- already-returned quantity tracking
- partial return
- full return
- refund method
- reason
- staff `PROCESS_RETURN` permission
- inventory restoration
- original batch restoration when cost allocations exist
- persistent return + return lines
- credit-sale Khata reversal

Relevant models include:

`ReturnEntity`  
`ReturnLineEntity`  
`ReturnCandidateLine`  
`ReturnDao`  
`ReturnRepository`

Do not bypass staff permission enforcement.

Repeated partial returns must never exceed the originally sold quantity.

---

## 13. Owner dashboard / reconciliation

The Home dashboard is data-driven and can show:

- today's sales
- bill count
- items sold
- cash
- UPI
- card
- Khata
- COGS
- gross profit
- expected cash
- counted cash
- cash difference

Direct actions are exposed for purchasing and returns.

Remaining reporting gaps:

- return-adjusted profit/sales
- purchases dashboard
- supplier payable summary
- customer receivables summary
- expenses
- richer reports

---

## 14. Staff / permissions

`StaffPermission` includes:

- APPLY_BILL_DISCOUNT
- APPLY_ITEM_DISCOUNT
- OVERRIDE_SELLING_PRICE
- VOID_BILL
- PROCESS_RETURN
- VIEW_REPORTS
- MANAGE_PRODUCTS
- MANAGE_STAFF
- CHANGE_STORE_SETTINGS

Do not put critical authorization only in Compose UI. Enforce at business/data boundaries too.

---

## 15. GST/tax model

Store configuration must distinguish at least:

- no GST registration
- regular GST
- composition

Do not assume every kirana collects GST separately.

Tax complexity should stay out of the cashier workflow when the store profile does not require it.

---

## 16. Stabilization audit performed on 2026-08-20

Before pausing development, a repository-wide static/debug pass was performed over:

- repository tree
- Android manifest/activities
- core business rules
- Room entities/DAOs/migrations
- POS/checkout
- purchase flow
- supplier ledger
- Khata
- inventory/FEFO
- returns/refunds
- COGS
- dashboard
- Intelligent Capture
- obvious placeholder/TODO markers

Concrete issues found during the stabilization pass and fixed:

1. Return repository referenced a nonexistent permission helper → aligned to actual `StaffPermission.PROCESS_RETURN`.
2. Dashboard referenced `getCogsTotal()` without the DAO query → added/verified query and cost allocations.
3. Return screen referenced `ReturnCandidateLine`; canonical model was found in `ReturnEntities.kt`; duplicate temporary model was removed.
4. Purchase UI could add duplicate products despite composite primary key → blocked.
5. Stale/nonexistent imports in return screen were cleaned.

No obvious `TODO`/`FIXME`/`NotImplemented` hits were returned by repository search during the pass.

---

## 17. What is NOT yet proven

This is critical:

**The assistant environment could not perform a real repository checkout/build because outbound access to `github.com` was unavailable.**

Therefore these are NOT yet proven:

- `./gradlew test`
- `./gradlew assembleDebug`
- actual APK install
- Room migration execution on a real installed database
- CameraX hardware behavior
- ML Kit hardware behavior
- microphone/voice behavior
- offline behavior after process death
- physical navigation/UX
- real refund/stock restoration behavior on device

Do not tell a future user that the app is compile-green or runtime-green until those are actually executed.

---

## 18. Windows + spare Android testing procedure

The user has **Windows** and a spare Android phone.

### Windows

Install Android Studio with SDK + Platform Tools.

Verify PowerShell:

```powershell
java -version
adb version
adb devices
```

### Phone

Enable:

```text
Developer options
→ USB debugging
```

Connect USB and accept the RSA authorization prompt.

### Build

From repo root:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Install debug APK:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Logs:

```powershell
adb logcat
```

### Physical acceptance test order

1. launch/login/staff gate
2. product create/edit
3. barcode + alternate barcode
4. no-barcode product
5. product metadata/image
6. POS search
7. barcode billing
8. voice loose-item billing
9. decimal quantity
10. discount/tax
11. cash
12. UPI
13. card
14. credit/Khata
15. receipt/share/re-open
16. hold bill
17. kill app
18. resume held bill
19. purchase/supplier
20. free-unit scheme
21. batch/expiry
22. FEFO
23. inventory adjustment/receive
24. customer payment/statement
25. return full
26. return partial twice
27. over-return rejection
28. refund
29. credit reversal
30. COGS/dashboard
31. cash reconciliation
32. airplane/offline test
33. Intelligent Capture barcode
34. OCR-only capture
35. multi-frame capture
36. local candidate
37. catalog candidate
38. local/catalog conflict
39. reject/keep-camera
40. create product
41. return created product to active bill
42. app restart/offline recovery

Capture failure details with Logcat.

---

## 19. GitHub Actions constraint

User has zero GitHub Actions minutes currently, expected recovery around **September 13, 2026**.

Until then:

- no CI debugging
- no workflow rerun loops
- no dummy commits for CI
- no temporary verification PRs
- no CI claims unless a real successful result exists

A status such as pending CodeRabbit is not Android build verification.

---

## 20. Definition of done

Feature is complete only when:

1. UI exists
2. business rules exist
3. persistence exists if required
4. navigation works
5. loading/error/empty states work
6. edge cases work
7. adjacent workflows integrate
8. tests exist
9. offline behavior is considered
10. docs/roadmap updated
11. local/physical validation completed where applicable
12. later deliberate CI verification passes

---

## 21. How a new development chat must start

Do NOT start coding immediately.

Read:

1. `README.md`
2. `docs/ROADMAP.md`
3. `docs/DEVELOPER_HANDOFF_V20.md` — this file
4. latest `retailpos-v2` commit
5. relevant current source files

Then report:

```text
Repository state:
Active branch:
Latest commit:
Room version:
Current roadmap priority:
Verified implemented pieces:
Potentially incomplete/unverified pieces:
Architecture concerns:
Testing status:
CI constraint:
Next test/development slice:
```

The repository remains the source of truth.

---

## 22. Current immediate task

**Do not add new product features yet.**

Run the real Windows/device validation first.

```text
Windows environment
↓
Gradle tests/build
↓
APK install
↓
physical acceptance matrix
↓
logcat/debug
↓
fix concrete failures
↓
retest
↓
then resume feature development
```

If the testing chat is interrupted, a new chat can resume from this file without needing the old conversation.
