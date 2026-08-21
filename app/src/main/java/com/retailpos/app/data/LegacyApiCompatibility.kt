package com.retailpos.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Compatibility bridge while the V2 repositories/DAOs are being consolidated. */
suspend fun SaleDao.checkout(
    storeId: String,
    cart: List<CartLine>,
    paymentMethod: String,
    idempotencyKey: String,
    customerId: String? = null,
    amountTendered: Double? = null,
    now: Long = System.currentTimeMillis(),
    taxTreatment: com.retailpos.app.core.products.TaxTreatment? = null,
    billDiscountAmount: Double = 0.0,
    staffRole: com.retailpos.app.core.permissions.StaffRole = com.retailpos.app.core.staff.StaffSessionStore.current()?.role
        ?: com.retailpos.app.core.permissions.StaffRole.CASHIER
): CheckoutResult = SaleCheckoutRepository(RetailDatabase.current()).checkout(
    storeId = storeId,
    cart = cart,
    paymentMethod = paymentMethod,
    idempotencyKey = idempotencyKey,
    customerId = customerId,
    amountTendered = amountTendered,
    now = now,
    taxTreatment = taxTreatment,
    billDiscountAmount = billDiscountAmount,
    staffRole = staffRole
)

fun InventoryDao.observeMovements(storeId: String, productId: String): Flow<List<InventoryMovementEntity>> = flow {
    emit(getProductMovements(storeId, productId))
}

@androidx.compose.runtime.Composable
fun CustomersScreen(
    storeId: String,
    dao: CustomerDao,
    khataDao: KhataDao,
    onOpenCustomer: (CustomerEntity) -> Unit,
    onBack: () -> Unit
) {
    CustomersScreen(storeId = storeId, onBack = onBack, onOpenKhata = { onOpenCustomer(it) })
}

@androidx.compose.runtime.Composable
fun CustomerKhataScreen(
    storeId: String,
    customer: CustomerEntity,
    dao: KhataDao,
    onBack: () -> Unit
) {
    CustomerKhataScreen(storeId = storeId, customerId = customer.id, onBack = onBack)
}
