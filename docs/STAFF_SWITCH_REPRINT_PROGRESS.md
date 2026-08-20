# Staff Switching + Receipt Reprint Progress

## Implemented

- Dashboard `SWITCH CASHIER` clears the process-local authenticated session and relaunches the staff gate.
- The staff gate authenticates the next cashier/manager/owner locally.
- Checkout continues using the authenticated role for discount authorization.
- Analytics exposes the 10 most recent persisted sales.
- Each recent sale can regenerate its receipt from persisted sale and sale-line data and open the Android share/reprint chooser.
- Receipt regeneration uses historical sale pricing/payment fields rather than current product settings.

## Deliberate boundaries

- Switching cashier is intended for use when the active cart has already been completed or abandoned; active-bill handoff is not implemented yet.
- The current reprint operation uses Android sharing/printing destinations. Thermal-printer integration remains separate.
- Printed-copy audit events are not yet persisted.
- Persistent last-user session storage is intentionally not used; the app asks for a PIN after process death.
