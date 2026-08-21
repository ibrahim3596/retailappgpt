package com.retailpos.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.retailpos.app.data.CustomerDao
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.InventoryMovementEntity
import com.retailpos.app.data.KhataDao
import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.RetailDatabase
import kotlinx.coroutines.launch

/** Transitional adapters for callers that still use the older screen signatures. */

@Composable
fun InventoryDetailScreen(
    product: ProductEntity,
    movements: List<InventoryMovementEntity>,
    onBack: () -> Unit,
    onAdjust: () -> Unit,
    onReceive: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    InventoryDetailScreen(
        product = product,
        batchesLoader = { database.inventoryDao().getAvailableBatchesFefo(product.storeId, product.id) },
        movementsLoader = { movements },
        onBack = onBack,
        onAdjust = onAdjust,
        onReceive = onReceive
    )
}

@Composable
fun InventoryAdjustmentScreen(
    productId: String,
    onBack: () -> Unit,
    error: String?,
    onSubmit: (String, Double, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    val product by produceState<ProductEntity?>(initialValue = null, productId) {
        value = database.productDao().getById(productId, "local-store")
    }
    product?.let { current ->
        InventoryAdjustmentScreen(
            product = current,
            onBack = onBack,
            onAdjust = { delta, reason -> onSubmit(productId, delta, reason) },
            error = error
        )
    }
}

@Composable
fun InventoryReceiveScreen(
    productId: String,
    onBack: () -> Unit,
    error: String?,
    onSubmit: (String, Double, String?, Long?, Double) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    val product by produceState<ProductEntity?>(initialValue = null, productId) {
        value = database.productDao().getById(productId, "local-store")
    }
    product?.let { current ->
        InventoryReceiveScreen(
            product = current,
            onBack = onBack,
            onReceive = { quantity, batch, expiry, purchasePrice ->
                onSubmit(productId, quantity, batch, expiry, purchasePrice)
            },
            error = error
        )
    }
}

@Composable
fun CustomersScreen(
    storeId: String,
    dao: CustomerDao,
    khataDao: KhataDao,
    onOpenCustomer: (CustomerEntity) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    CustomersScreen(
        storeId = storeId,
        onBack = onBack,
        onOpenKhata = { customerId ->
            scope.launch {
                dao.getById(customerId, storeId)?.let(onOpenCustomer)
            }
        }
    )
}

@Composable
fun CustomerKhataScreen(
    storeId: String,
    customer: CustomerEntity,
    dao: KhataDao,
    onBack: () -> Unit
) {
    CustomerKhataScreen(storeId = storeId, customerId = customer.id, onBack = onBack)
}
