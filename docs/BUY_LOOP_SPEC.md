# RetailPOS V2 — Buy Loop Specification

## Goal

Model the actual kirana/general-trade purchasing cycle without conflating purchase cost, stock quantity, schemes, or supplier credit.

## Flow

Supplier
→ Purchase invoice
→ Purchase lines
→ Scheme/free quantity
→ Batch/expiry where applicable
→ Stock increase
→ Supplier payable

## Supplier

Required concepts:

- stable supplier ID
- store ID
- supplier name
- phone/contact
- address
- notes
- active/inactive state later

## Purchase

Required concepts:

- stable purchase ID
- store ID
- supplier ID
- supplier invoice number when available
- created/received timestamp
- gross/net total
- paid amount
- outstanding payable

## Purchase line

Required concepts:

- product ID
- ordered/paid quantity
- free quantity
- stock quantity = paid + free
- purchase rate
- gross cost
- scheme discount
- net cost
- effective unit cost
- optional batch number
- optional expiry date

## Scheme semantics

Example:

10 units purchased × ₹100 = ₹1,000 paid cost
1 unit free

Received stock = 11 units
Net cost = ₹1,000
Effective cost = ₹90.91/unit

Free units increase stock but do not increase paid cost.

## Expiry/batch rule

Expiry requires a batch/lot identifier in the purchase workflow so batch-level stock remains traceable.

Products without meaningful batch/expiry information can still be received without inventing one.

## Supplier payable

Supplier balance = purchase net total - amount already paid.

Allowed states:

- OUTSTANDING
- SETTLED
- INVALID

Overpayment is rejected until a deliberate supplier-credit/refund model exists.

## Persistence

Room now contains the supplier/purchase domain at database version 19:

- `suppliers`
- `purchases`
- `purchase_lines`
- `supplier_ledger`

Migrations `17 → 18 → 19` are registered. Held bills are persistent at 17→18; purchasing tables are introduced by 18→19.

## Atomic purchase posting

`PurchaseRepository.recordPurchase()` posts purchase, lines, stock, batches, inventory movement and supplier-ledger entries inside one Room transaction.

## UI

`PurchaseActivity` + `PurchaseEntryScreen` now provide:

- supplier selection
- add supplier
- invoice number
- multiple purchase lines
- paid/free quantity
- purchase rate
- scheme discount
- batch/expiry
- supplier payment
- live gross/net/outstanding totals
- atomic receive/post action

The current UI intentionally avoids a separate ERP-style purchase-order workflow.

## Relationship to COGS

The effective purchase cost is the future cost basis for inventory valuation and COGS. Free quantities therefore matter to profit reporting even though they are not separately paid for.

## Remaining work

- supplier list/history UI
- supplier payment/statement UI
- purchase history/details UI
- purchase edit/correction policy
- COGS and inventory valuation integration
- return-to-supplier workflow
- purchase import/scan automation
- supplier/product association shortcuts
