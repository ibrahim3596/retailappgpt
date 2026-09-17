package com.example.retailpos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.ui.theme.*
import com.example.ui.theme.RetailColors
import com.example.ui.theme.Spacing
import com.example.ui.theme.IconSizes

@Composable
fun CartItemRow(
    name: String,
    brand: String,
    packSize: String,
    price: Double,
    quantity: Double,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = Spacing.md, horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$brand • $packSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(Shapes.small)
                        .background(SurfaceVariant)
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(IconSizes.md), tint = TextPrimary)
                    }
                    Text(
                        // Weight-based items carry fractional quantities; truncating
                        // to Int would display 0.5 kg as "0".
                        text = if (quantity % 1.0 == 0.0) quantity.toLong().toString()
                               else String.format(java.util.Locale.US, "%.2f", quantity).trimEnd('0').trimEnd('.'),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(IconSizes.md), tint = TextPrimary)
                    }
                }

                Text(
                    text = "₹${String.format("%.0f", price * quantity)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    modifier = Modifier.widthIn(min = 70.dp)
                )
            }
        }
    }
}

@Composable
fun PosSummaryRow(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Black else FontWeight.Medium,
            color = if (isTotal) TextPrimary else TextSecondary
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            fontWeight = if (isTotal) FontWeight.Black else FontWeight.Bold,
            color = if (isTotal) Primary else TextPrimary
        )
    }
}

@Composable
fun EmptyCartState(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = SurfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ShoppingBasket,
                    contentDescription = "Empty cart",
                    modifier = Modifier.size(IconSizes.xxxl),
                    tint = TextTertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            "Your bill is empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            "Scan a barcode or search for a product to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xxl)
        )
        Spacer(modifier = Modifier.height(Spacing.xxl))
        Button(
            onClick = onScanClick,
            shape = Shapes.pill,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = Spacing.xxxl)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
            Spacer(modifier = Modifier.width(Spacing.md))
            Text("SCAN BARCODE", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.level1
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = Shapes.medium,
                color = SurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = "Product",
                        modifier = Modifier.size(IconSizes.lg),
                        tint = Primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.brand}${if (product.brand.isNotEmpty()) " • " else ""}${product.variant}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Surface(
                        color = SurfaceVariant,
                        shape = Shapes.extraSmall
                    ) {
                        Text(
                            text = product.barcode,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${product.sellingPrice}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Primary
                    )
                }
                Text(
                    text = "MRP ₹${product.mrp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                val stockStatus = when {
                    product.currentStock <= 0 -> "Out of Stock"
                    product.currentStock <= product.minStock -> "Low Stock"
                    else -> "In Stock"
                }
                val statusColor = when {
                    product.currentStock <= 0 -> Error
                    product.currentStock <= product.minStock -> Warning
                    else -> Success
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = Shapes.pill
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = "${product.currentStock.toInt()} ${product.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}