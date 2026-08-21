# RetailPOS V2 — Physical Phone Testing Gate

## Gate status

**Code preparation: READY FOR PHYSICAL TESTING**

This does **not** mean the APK has been built or executed in this environment. The repository has been prepared for the first real Android-device pass.

## Build environment

Current Android configuration:

- application ID: `com.retailpos.app`
- min SDK: 26
- target SDK: 36
- compile SDK: 36
- Java/JVM target: 17
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.2.10
- required Gradle: 8.13 for AGP 8.13
- Room: 2.8.4
- CameraX: 1.6.1
- ML Kit barcode/text/image labeling enabled

The repository currently does not contain a Gradle wrapper. Android Studio or another Android build environment must therefore supply/configure Gradle 8.13 rather than relying on `./gradlew`.

## Instrumentation gate

An Android instrumentation smoke test has been added:

```text
app/src/androidTest/java/com/retailpos/app/data/RetailDatabaseSmokeTest.kt
```

It verifies on a real/emulated device that:

1. the application database opens;
2. the Room schema is version 25;
3. SQLite `PRAGMA quick_check` returns `ok`;
4. the database remains open after validation.

## First physical test sequence

### 1. Fresh install

Install the debug build on the phone.

Expected:

- app launches into owner setup;
- create owner account with a 4–8 digit PIN;
- successful setup enters the POS shell;
- reopening the app shows staff login instead of owner setup.

### 2. Product creation

Create:

- one normal piece product;
- one decimal/loose-goods product;
- one product with barcode;
- one product without barcode.

Verify prices, unit, stock and product details persist after leaving/reopening the screen.

### 3. Barcode

Grant camera permission.

Test:

- known barcode → exact local product;
- unknown barcode → identify/add-manually path;
- arbitrary QR → rejected as normal product identity;
- rapid repeated scan → no duplicate cart insertion.

### 4. POS

Test:

- search → add;
- barcode → add;
- quantity increment/decrement;
- decimal quantity;
- bill discount;
- item pricing/discount authorization;
- clear bill;
- hold bill;
- resume bill.

### 5. Checkout

Run separate transactions for:

- cash with exact amount;
- cash with excess tender and change;
- UPI;
- card;
- CREDIT/Khata;
- insufficient stock;
- expired-only stock;
- retry after a failed checkout.

Verify after each successful sale:

```text
sale persisted
stock decreased exactly once
inventory movement exists
receipt is generated
```

For CREDIT:

```text
customer balance increases exactly by sale total
```

### 6. Purchase / receive

Create a supplier and receive a batch.

Verify:

```text
supplier payable increases by net purchase amount
stock increases by paid + free quantity
batch exists
expiry is stored
inventory movement points to that batch
```

### 7. Return

Perform both partial and full returns.

Verify:

```text
refund is recorded
stock returns
original batch is restored when batch cost allocation exists
inventory movement retains the batch identity
CREDIT return reduces Khata balance
```

### 8. Recovery/offline

With a bill open:

- background the app;
- force-stop/reopen where practical;
- confirm active cart recovery;
- confirm no duplicate checkout after retry.

Repeat essential Product/POS/Khata operations with network unavailable. Core local flows must continue to work.

### 9. Analytics

After sales, returns, purchases and expenses, verify:

- net sales;
- net items;
- COGS;
- operating result;
- payment mix after refunds;
- top sellers after returned quantities/revenue;
- receivables;
- supplier payables;
- near-expiry/inventory valuation where surfaced.

## Physical-device acceptance criteria

The first phone pass is considered successful when:

- app installs and launches;
- owner setup works;
- staff login works;
- camera permission/scanner works;
- barcode add-to-cart works;
- QR is not treated as a normal product;
- product create/edit persists;
- cash checkout works;
- UPI/card validation works;
- CREDIT checkout updates Khata;
- stock decreases once per sale;
- hold/resume works;
- purchase receive increases stock and payable correctly;
- return restores stock and refund correctly;
- app restart does not duplicate transactions;
- analytics remains internally consistent;
- no crash occurs during the critical SELL/BUY/CREDIT loops.

## Known pre-build limitations

These remain deliberately outside the first physical gate:

- thermal printer integration;
- cloud synchronization;
- multi-store infrastructure;
- advanced image-recognition service;
- enterprise forecasting;
- full migration-chain instrumentation beyond the device smoke gate.

## Verification discipline

Do not mark this gate as fully passed from source inspection alone. A real device/emulator run must be performed and the results recorded after installation.
