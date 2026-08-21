package com.retailpos.app.data

import com.retailpos.app.core.payment.PendingPaymentStore
import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.products.TaxTreatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Transitional adapters for the current V2 UI callers. */
suspend fun SaleDao.checkout(
    storeId: String,
    cart: List<CartLine>,
    paymentMethod: String,
    idempotencyKey: String,
    customerId: String? = null,
    amountTendered: Double? = null,
    now: Long = System.currentTimeMillis(),
    taxTreatment: TaxTreatment? = null,
    billDiscountAmount: Double = 0.0,
    staffRole: StaffRole = com.retailpos.app.core.staff.StaffSessionStore.current()?.role ?: StaffRole.CASHIER
): CheckoutResult = SaleCheckoutRepository(RetailDatabase.get(PendingPaymentStore.context()))
    .checkout(storeId, cart, paymentMethod, idempotencyKey, customerId, amountTendered, now, taxTreatment, billDiscountAmount, staffRole)

fun InventoryDao.observeMovements(storeId: String, productId: String? = null): Flow<List<InventoryMovementEntity>> = flow {
    emit(if (productId == null) getMovements(storeId) else getProductMovements(storeId, productId))
}
