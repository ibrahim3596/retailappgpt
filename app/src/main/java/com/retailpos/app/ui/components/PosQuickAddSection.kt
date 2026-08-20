package com.retailpos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.pos.FavoriteProductStore
import com.retailpos.app.core.pos.QuickAddProduct

@Composable
fun PosQuickAddSection(
    title: String,
    products: List<QuickAddProduct>,
    onAdd: (QuickAddProduct) -> Unit,
    onToggleFavorite: (QuickAddProduct) -> Unit = { FavoriteProductStore.toggle(it.productId) }
) {
    if (products.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products, key = { it.productId }) { product ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = { if (product.canAdd) onAdd(product) },
                        label = { Text("${product.name} ₹${"%.2f".format(product.sellingPrice)}") },
                        enabled = product.canAdd
                    )
                    FilterChip(
                        selected = FavoriteProductStore.isFavorite(product.productId),
                        onClick = { onToggleFavorite(product) },
                        label = { Text("★") }
                    )
                }
            }
        }
    }
}
