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
- created/received timestamps
- purchase status later if draft/posted is needed
- gross/net total
- paid amount
- outstanding payable
- notes

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

Expiry should require a batch/lot identifier in the purchase workflow so batch-level stock remains traceable.

Products without meaningful batch/expiry information must still be receivable without inventing one.

## Supplier payable

Supplier balance = purchase net total - amount already paid.

Allowed states:

- OUTSTANDING
- SETTLED
- INVALID

Overpayment should be rejected until a deliberate supplier-credit/refund model exists.

## Relationship to existing inventory

Posting a purchase must eventually create:

1. product stock increase
2. inventory batch when applicable
3. inventory movement with reason RECEIVE/PURCHASE
4. supplier payable transaction

These changes should be one Room transaction.

## Relationship to COGS

The effective purchase cost becomes the cost basis for inventory valuation and, later, COGS calculations.

Free quantities therefore matter to profit reporting even though they are not separately paid for.

## Current implementation boundary

The domain models and deterministic rules exist now without persistence. Room entities/migrations are intentionally deferred until the database registration path can be changed safely.

Do not create a UI-only supplier database or process-local purchase history and call it complete.
