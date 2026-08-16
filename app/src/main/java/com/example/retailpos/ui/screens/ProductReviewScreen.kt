package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.TaxType
import com.example.retailpos.data.local.entity.VerificationStatus
import com.example.retailpos.engine.ai.GeminiVisionFallback
import com.example.retailpos.engine.barcode.BarcodeNormalizer
import com.example.retailpos.engine.ocr.PackagingOcrParser
import com.example.retailpos.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewScreen(
    viewModel: MainViewModel,
    barcode: String = "",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var sampleOcrInputText by remember {
        mutableStateOf(
            "AMUL TAAZA TONED MILK\nNet Wt. 500ml\nM.R.P. Rs. 28.00\nMfd: 01/2026\nHSN 0401\nGST 5%\n$barcode"
        )
    }

    var existingProductId by remember { mutableStateOf<String?>(null) }
    var sku by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

    var productName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var packSize by remember { mutableStateOf("") }
    var currentBarcode by remember { mutableStateOf(barcode) }
    var mrpText by remember { mutableStateOf("") }
    var sellingPriceText by remember { mutableStateOf("") }
    var purchasePriceText by remember { mutableStateOf("") }
    var gstRateText by remember { mutableStateOf("5") }
    var hsnCode by remember { mutableStateOf("") }
    var initialStockText by remember { mutableStateOf("50") }

    var isOcrProcessed by remember { mutableStateOf(false) }
    var mrpConflictDetected by remember { mutableStateOf(false) }

    fun runRealOcrParser() {
        val lines = sampleOcrInputText.lines().filter { it.isNotBlank() }
        val ocrResult = PackagingOcrParser.parsePackagingText(lines)
        val aiResult = GeminiVisionFallback.parsePackagingWithAi(ocrResult)

        productName = aiResult.productName ?: ""
        brand = aiResult.brand ?: ""
        packSize = aiResult.packSize ?: ""
        currentBarcode = ocrResult.barcode ?: barcode
        mrpText = aiResult.mrp?.toString() ?: "28.0"
        sellingPriceText = (aiResult.mrp ?: 28.0).toString()
        purchasePriceText = ((aiResult.mrp ?: 28.0) * 0.85).toInt().toString()
        gstRateText = (aiResult.gstRate ?: 5.0).toInt().toString()
        hsnCode = aiResult.hsnCode ?: "0401"

        mrpConflictDetected = ocrResult.mrpConflictDetected
        isOcrProcessed = true
    }

    LaunchedEffect(barcode, products) {
        if (barcode.isNotBlank()) {
            val existing = products.find { it.barcode == barcode || it.normalizedBarcode == BarcodeNormalizer.normalize(barcode).canonicalGtin }
            if (existing != null) {
                existingProductId = existing.id
                sku = existing.sku
                category = existing.category
                productName = existing.name
                brand = existing.brand
                packSize = existing.variant
                currentBarcode = existing.barcode
                mrpText = existing.mrp.toString()
                sellingPriceText = existing.sellingPrice.toString()
                purchasePriceText = existing.purchasePrice.toString()
                gstRateText = existing.gstRate.toInt().toString()
                hsnCode = existing.hsnCode
                initialStockText = existing.currentStock.toInt().toString()
                isOcrProcessed = true
            } else {
                // Pre-fill barcode for new product
                currentBarcode = barcode
                existingProductId = null
                sku = ""
                // Auto-run OCR simulation if barcode is provided but not found
                runRealOcrParser()
            }
        } else {
            existingProductId = null
            sku = ""
        }
    }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (existingProductId != null) "EDIT PRODUCT" else "ADD PRODUCT", 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 1.sp, 
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Info
            if (existingProductId != null) {
                Surface(
                    color = RetailPrimary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RetailPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "You are editing an existing product in your catalogue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RetailPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // OCR Recognition Section (Only show if new product or explicitly requested)
            if (existingProductId == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RetailSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = RetailPrimary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp), tint = RetailPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("PACKAGING SCAN", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RetailTextPrimary)
                        }

                        OutlinedTextField(
                            value = sampleOcrInputText,
                            onValueChange = { sampleOcrInputText = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            label = { Text("Recognized Text Buffer", style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = RetailSurface, focusedContainerColor = RetailSurface)
                        )

                        Button(
                            onClick = { runRealOcrParser() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailPrimary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUTO-EXTRACT DETAILS", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (mrpConflictDetected) {
                    Surface(
                        color = RetailError.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RetailError.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RetailError)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("MRP discrepancy found in scan. Verify manually.", style = MaterialTheme.typography.bodySmall, color = RetailError, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Shopkeeper Verification Form
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Product Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                    OutlinedTextField(
                        value = packSize,
                        onValueChange = { packSize = it },
                        label = { Text("Variant") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                }

                Divider(color = RetailBorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                Text("Identification & Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU / Item Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                )

                OutlinedTextField(
                    value = currentBarcode,
                    onValueChange = { currentBarcode = it },
                    label = { Text("Barcode") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = RetailTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                )

                Divider(color = RetailBorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                Text("Pricing & Tax", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = mrpText,
                        onValueChange = { mrpText = it },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("Selling Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = purchasePriceText,
                        onValueChange = { purchasePriceText = it },
                        label = { Text("Purchase (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                    OutlinedTextField(
                        value = gstRateText,
                        onValueChange = { gstRateText = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                }

                Divider(color = RetailBorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                Text("Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                    OutlinedTextField(
                        value = initialStockText,
                        onValueChange = { initialStockText = it },
                        label = { Text(if (existingProductId != null) "Current Stock" else "Opening Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RetailPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val mrp = mrpText.toDoubleOrNull() ?: 0.0
                    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
                    val purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0
                    val gstRate = gstRateText.toDoubleOrNull() ?: 0.0
                    val stock = initialStockText.toDoubleOrNull() ?: 0.0

                    if (productName.isBlank() || currentBarcode.isBlank() || mrp <= 0.0) {
                        Toast.makeText(context, "Product Name, Barcode & MRP are required!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (sellingPrice > mrp) {
                        Toast.makeText(context, "Selling price cannot exceed MRP!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val storeId = store?.id ?: "STORE-DEFAULT-001"
                    val normalized = BarcodeNormalizer.normalize(currentBarcode).canonicalGtin

                    scope.launch {
                        val finalSku = if (sku.isBlank()) {
                            var candidate: String
                            var isUnique = false
                            var attempts = 0
                            do {
                                candidate = "SKU-" + UUID.randomUUID().toString().take(6).uppercase()
                                if (viewModel.inventoryRepo.getProductBySku(storeId, candidate) == null) {
                                    isUnique = true
                                }
                                attempts++
                            } while (!isUnique && attempts < 10)
                            candidate
                        } else {
                            // If manually entered, check for uniqueness if it's a new product or SKU changed
                            if (existingProductId == null || sku != products.find { it.id == existingProductId }?.sku) {
                                if (viewModel.inventoryRepo.getProductBySku(storeId, sku) != null) {
                                    Toast.makeText(context, "SKU already exists!", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                            }
                            sku
                        }

                        val updatedProduct = ProductEntity(
                            id = existingProductId ?: UUID.randomUUID().toString(),
                            storeId = storeId,
                            sku = finalSku,
                            barcode = currentBarcode,
                            normalizedBarcode = normalized,
                            name = productName,
                            brand = brand,
                            variant = packSize,
                            packSize = packSize,
                            category = category,
                            hsnCode = hsnCode,
                            unit = "PCS",
                            mrp = mrp,
                            sellingPrice = sellingPrice,
                            purchasePrice = purchasePrice,
                            gstRate = gstRate,
                            taxType = TaxType.INCLUSIVE,
                            currentStock = stock,
                            verificationStatus = VerificationStatus.VERIFIED,
                            lastVerifiedAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )

                        viewModel.saveProductWithAuth(updatedProduct) { success ->
                            if (success) {
                                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Unauthorized operation", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RetailPrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (existingProductId != null) "UPDATE PRODUCT" else "SAVE PRODUCT TO CATALOGUE", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 16.sp
                )
            }
        }
    }
}
