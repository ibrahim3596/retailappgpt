package com.retailpos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.ProductBarcodeEntity
import com.retailpos.app.data.ProductEntity

@Composable
fun ProductIdentitySummary(
    product: ProductEntity,
    barcodes: List<ProductBarcodeEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (product.brand.isNotBlank()) Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU $it", style = MaterialTheme.typography.labelMedium) }
            Text("${barcodes.size} identifier${if (barcodes.size == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium)
        }
        barcodes.firstOrNull { it.isPrimary }?.let {
            Text("Primary barcode ${it.value}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
