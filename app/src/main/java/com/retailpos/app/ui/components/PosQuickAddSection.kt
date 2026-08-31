package com.retailpos.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.pos.FavoriteProductStore
import com.retailpos.app.core.pos.QuickAddProduct
import java.util.Locale

@Composable
fun PosQuickAddSection(
    title: String,
    products: List<QuickAddProduct>,
    onAdd: (QuickAddProduct) -> Unit,
    onToggleFavorite: (QuickAddProduct) -> Unit = { FavoriteProductStore.toggle(it.productId) },
    isFavorite: (QuickAddProduct) -> Boolean = { FavoriteProductStore.isFavorite(it.productId) }
) {
    if (products.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products.take(12), key = { it.productId }) { product ->
                val favorite = isFavorite(product)
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .width(164.dp)
                        .clickable(enabled = product.canAdd) { onAdd(product) }
                        .semantics {
                            contentDescription = if (product.canAdd) {
                                "Add ${product.name}, ${formatPrice(product.sellingPrice)}"
                            } else {
                                "${product.name}, out of stock"
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 13.dp, end = 5.dp, top = 11.dp, bottom = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(
                                formatPrice(product.sellingPrice),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { onToggleFavorite(product) },
                            modifier = Modifier.semantics { contentDescription = if (favorite) "Remove ${product.name} from favorites" else "Add ${product.name} to favorites" }
                        ) {
                            Icon(
                                imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatPrice(value: Double): String = String.format(Locale.US, "₹%.2f", value)
