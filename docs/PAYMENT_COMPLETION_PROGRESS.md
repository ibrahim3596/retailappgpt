# Payment Completion Progress

## Implemented

- Cash tender validation
- Cash change calculation
- UPI exact-payable validation
- Card exact-payable validation
- Credit payment handling
- Persisted amount tendered on sales
- Persisted change amount on sales
- Room migration 15 -> 16
- Checkout transaction validates payment settlement before stock mutation
- Receipt shows subtotal, discount, GST, total and cash settlement details
- Payment settlement unit tests
- Receipt settlement formatting test

## Deliberate boundary

UPI/card completion currently records the selected payment method and validates the payable amount, but does not claim external payment-provider confirmation. A future payment integration must add a provider-specific confirmation/reference field and only finalize the sale after the provider reports success.

Cash is fully local/offline: the shopkeeper enters the amount received and RetailPOS calculates and persists change.
