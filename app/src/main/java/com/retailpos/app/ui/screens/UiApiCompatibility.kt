package com.retailpos.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.core.products.TaxTreatment
import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.RetailDatabase

/** Transitional adapters for callers that still use the older screen signatures. */

fun StoreTaxMode.toTaxTreatment(): TaxTreatment = when (this) {
    StoreTaxMode.NO_GST, StoreTaxMode.COMPOSITION -> TaxTreatment.NO_TAX
    StoreTaxMode.REGULAR -> TaxTreatment.GST_ADDED
}

@Composable
fun InventoryDetailScreen(
    product: ProductEntity,
    movements: List<com.retailpos.app.data.InventoryMovementEntity>,
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
