# RetailPOS V2 — POS Permissions & Payment Progress

## Verified implementation

### Staff permissions

- `StaffRole`: OWNER, MANAGER, CASHIER
- Central `StaffPermissionRules` for bill discounts, item discounts, price overrides, voids, returns, reports, product management, staff management, and store settings.
- Bill discount limits are currently defined as OWNER 100%, MANAGER 50%, CASHIER 10%.
- Selling-price override is allowed for OWNER/MANAGER but not CASHIER.
- `SaleDao.checkout()` now validates the supplied staff role against the requested bill discount before any inventory mutation or sale persistence.

### Important boundary

A real staff identity/login/session system does not yet exist. `SaleDao.checkout()` therefore defaults to `OWNER` for backward compatibility with the current single-user application. This is intentional and must be replaced by an authenticated local staff session before the role limits can be considered production security.

## Payment business rules

`PaymentRules` validates:

- Cash: tender must cover the final total and change is calculated.
- UPI/Card: tendered amount must match the payable total within ₹0.01.
- Credit: no cash tender is required.

Tests cover insufficient cash, change calculation, electronic payment mismatch, and credit sales.

## Not yet integrated

Cash amount tendered/change is not yet persisted on `sales` and is not yet wired through the checkout UI/transaction boundary. Do not claim cash/change checkout complete until the sale schema, DAO transaction, CheckoutScreen and receipt are updated together.

Next payment slice: persist amount tendered/change with a Room migration, pass the approved payment amount through checkout, show change in the UI, and include it on receipts.
