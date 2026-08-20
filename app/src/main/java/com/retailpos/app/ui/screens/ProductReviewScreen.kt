package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retailpos.app.core.products.PackCompatibility
import com.retailpos.app.core.products.ParsedPack
import com.retailpos.app.core.products.ProductCaptureObservation
import com.retailpos.app.core.products.ProductIdentificationRanking
import com.retailpos.app.core.products.ProductIdentificationSignals
import com.retailpos.app.core.products.ProductLocalCandidate
import com.retailpos.app.core.products.ProductPackCompatibility
import com.retailpos.app.data.BarcodeMutationResult
import com.retailpos.app.data.CatalogProduct
import com.retailpos.app.data.ProductCatalogLookup
import com.retailpos.app.data.ProductViewModel
import com.retailpos.app.data.ProductViewModelFactory
import com.retailpos.app.data.SaveProductResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewScreen(
    storeId: String,
    productId: String?,
    initialBarcode: String = "",
    autoIdentify: Boolean = false,
    onBack: () -> Unit,
    onSaved: ((String) -> Unit)? = null,
    onExistingProductSelected: ((String) -> Unit)? = null
) {
    val factory = remember(storeId) { ProductViewModelFactory(storeId) }
    val viewModel: ProductViewModel = viewModel(factory = factory)
    val editingProduct by viewModel.editingProduct.collectAsState()
    val barcodes by viewModel.barcodes.collectAsState()
    val localCandidates by viewModel.localCandidates.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(initialBarcode) }
    var sku by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var lowStockThreshold by remember { mutableStateOf("5") }
    var secondaryBarcode by remember { mutableStateOf("") }
    var secondaryBarcodeError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var captureHint by remember { mutableStateOf<String?>(null) }
    var catalogStatus by remember { mutableStateOf<String?>(null) }
    var localCandidateStatus by remember { mutableStateOf<String?>(null) }
    var identificationStatus by remember { mutableStateOf<String?>(null) }
    var identificationExplanation by remember { mutableStateOf<String?>(null) }
    var catalogCandidate by remember { mutableStateOf<CatalogProduct?>(null) }
    var identificationConfidence by remember { mutableStateOf<Int?>(null) }
    var captureObservation by remember { mutableStateOf<ProductCaptureObservation?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showIntelligentCapture by remember { mutableStateOf(false) }

    LaunchedEffect(productId, initialBarcode, autoIdentify) {
        viewModel.loadProduct(productId)
        viewModel.clearLocalCaptureCandidates()
        if (productId == null && initialBarcode.isNotBlank()) barcode = initialBarcode.trim()
        if (productId == null && autoIdentify) showIntelligentCapture = true
    }
    LaunchedEffect(editingProduct?.id) {
        editingProduct?.let { product ->
            name = product.name
            brand = product.brand
            barcode = product.barcode.orEmpty()
            sku = product.sku.orEmpty()
            mrp = product.mrp.toString()
            sellingPrice = product.sellingPrice.toString()
            purchasePrice = product.purchasePrice.toString()
            stock = product.stock.toString()
            unit = product.unit
            lowStockThreshold = product.lowStockThreshold.toString()
            captureObservation = null
            errorMessage = null
            catalogCandidate = null
            catalogStatus = null
            localCandidateStatus = null
            identificationStatus = null
            identificationExplanation = null
            identificationConfidence = null
        }
    }
    val isEdit = productId != null

    fun applyLocalCandidate(candidate: ProductLocalCandidate) {
        onExistingProductSelected?.invoke(candidate.product.id)
            ?: run { errorMessage = "This product already exists in your product master. Open it from Product Master instead of creating a duplicate." }
    }

    fun persistProduct() {
        val captured = captureObservation
        val capturedPack = captured?.pack
        if (capturedPack != null) {
            val compatibility = ProductPackCompatibility.classify(capturedPack, unit)
            if (compatibility.compatibility == PackCompatibility.MISMATCH_REQUIRES_REVIEW) {
                errorMessage = "Observed ${capturedPack.sourceText} does not match the selling unit ‘$unit’. Review the unit before saving."
                return
            }
        }
        val mrpValue = mrp.toDoubleOrNull() ?: -1.0
        val saleValue = sellingPrice.toDoubleOrNull() ?: -1.0
        val purchaseValue = purchasePrice.toDoubleOrNull() ?: -1.0
        val stockValue = stock.toDoubleOrNull() ?: -1.0
        val thresholdValue = lowStockThreshold.toDoubleOrNull() ?: -1.0
        val persistenceObservation = captured?.copy(categoryHint = null, categoryConfidence = null)
        viewModel.saveProduct(
            productId = productId,
            name = name,
            brand = brand,
            barcode = barcode,
            sku = sku,
            mrp = mrpValue,
            sellingPrice = saleValue,
            purchasePrice = purchaseValue,
            stock = stockValue,
            unit = unit,
            lowStockThreshold = thresholdValue,
            captureObservation = persistenceObservation
        ) { result ->
            when (result) {
                SaveProductResult.Success -> onSaved?.invoke(productId ?: "") ?: onBack()
                SaveProductResult.DuplicateSku -> errorMessage = "That SKU is already used by another product in this store."
                SaveProductResult.DuplicateBarcode -> errorMessage = "That barcode is already assigned to another product in this store."
                SaveProductResult.InvalidInput -> errorMessage = "Check the product name and numbers. Sale price must not exceed MRP, and stock/threshold cannot be negative."
                SaveProductResult.Error -> errorMessage = "Unable to save the product. Please try again."
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerScreen("SCAN PRODUCT BARCODE", { showBarcodeScanner = false }) { raw, _ ->
            barcode = raw.trim()
            errorMessage = null
            showBarcodeScanner = false
            val score = ProductIdentificationRanking.score(ProductIdentificationSignals(barcodeDetected = true))
            identificationStatus = "IDENTIFICATION: BARCODE"
            identificationConfidence = score.score
            identificationExplanation = score.explanation
        }
        return
    }

    if (showIntelligentCapture) {
        IntelligentProductCaptureScreen(
            onBack = { showIntelligentCapture = false },
            onResult = { result ->
                result.barcode?.let { barcode = it }
                result.detectedName?.let { name = it }
                result.detectedBrand?.let { brand = it }
                result.detectedMrp?.let { mrp = it.toString() }
                val observation = ProductCaptureObservation(
                    barcode = result.barcode,
                    printedName = result.detectedName,
                    printedBrand = result.detectedBrand,
                    mrp = result.detectedMrp,
                    categoryHint = result.categoryHint,
                    categoryConfidence = result.labelConfidence,
                    pack = if (result.detectedPackSize != null && !result.detectedPackUnit.isNullOrBlank()) ParsedPack(result.detectedPackSize, result.detectedPackUnit, "${result.detectedPackSize} ${result.detectedPackUnit}") else null,
                    frameCount = result.frameCount
                )
                captureObservation = observation
                val hasBarcode = !observation.barcode.isNullOrBlank()
                val hasText = !observation.printedName.isNullOrBlank() || !observation.printedBrand.isNullOrBlank()
                val hasVisual = observation.categoryHint != null
                val packCompatible = observation.pack?.let { ProductPackCompatibility.classify(it, unit).compatibility != PackCompatibility.MISMATCH_REQUIRES_REVIEW } ?: false
                val baseScore = ProductIdentificationRanking.score(ProductIdentificationSignals(barcodeDetected = hasBarcode, printedTextDetected = hasText, visualHintDetected = hasVisual, packCompatibleWithSellingUnit = packCompatible))
                identificationConfidence = baseScore.score
                identificationStatus = when {
                    hasBarcode && hasText -> "IDENTIFICATION: BARCODE + CAMERA/OCR"
                    hasBarcode -> "IDENTIFICATION: BARCODE"
                    hasText -> "IDENTIFICATION: CAMERA/OCR"
                    hasVisual -> "IDENTIFICATION: VISUAL HINT ONLY"
                    else -> "IDENTIFICATION: LIMITED EVIDENCE"
                }
                identificationExplanation = baseScore.explanation
                captureHint = buildString {
                    observation.categoryHint?.let { hint -> append("Visual hint: $hint"); observation.categoryConfidence?.let { append(" (${String.format(java.util.Locale.US, "%.0f%%", it * 100f)})") }; append(". ") }
                    observation.pack?.let { pack -> append("Observed pack: ${pack.sourceText}. Verify against selling unit.") }
                    if (observation.frameCount > 1) append(" Evidence across ${observation.frameCount} frames.")
                }.ifBlank { null }

                localCandidateStatus = "Checking your local product master…"
                viewModel.findLocalCaptureCandidates(observation.printedName, observation.printedBrand) { candidates ->
                    if (candidates.isEmpty()) localCandidateStatus = "No strong local product match found."
                    else {
                        localCandidateStatus = "Local product matches found. Review before creating a new product."
                        val top = candidates.first()
                        val localScore = ProductIdentificationRanking.score(ProductIdentificationSignals(barcodeDetected = hasBarcode, printedTextDetected = hasText, textAgreesWithCandidate = top.score >= 80, multipleFrameAgreement = observation.frameCount >= 2, packCompatibleWithSellingUnit = packCompatible))
                        identificationConfidence = maxOf(identificationConfidence ?: 0, localScore.score)
                        identificationExplanation = "${localScore.explanation} ${top.explanation}"
                    }
                }

                val detectedBarcode = result.barcode
                if (!detectedBarcode.isNullOrBlank()) {
                    catalogStatus = "Checking public product catalog…"
                    catalogCandidate = null
                    scope.launch {
                        val catalog = withContext(Dispatchers.IO) { ProductCatalogLookup.lookupByBarcode(detectedBarcode) }
                        if (catalog != null) {
                            catalogCandidate = catalog
                            catalogStatus = "Catalog match found. Review it before applying catalog identity."
                            val catalogScore = ProductIdentificationRanking.score(ProductIdentificationSignals(barcodeDetected = true, catalogMatched = true, printedTextDetected = hasText, textAgreesWithCandidate = hasText, multipleFrameAgreement = observation.frameCount >= 2, packCompatibleWithSellingUnit = packCompatible))
                            identificationConfidence = maxOf(identificationConfidence ?: 0, catalogScore.score)
                            identificationExplanation = catalogScore.explanation
                        } else catalogStatus = "No public catalog match. Using camera/OCR detection only."
                    }
                } else {
                    catalogStatus = null
                    catalogCandidate = null
                }
                errorMessage = null
                showIntelligentCapture = false
            }
        )
        return
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (isEdit) "EDIT PRODUCT" else "ADD PRODUCT", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isEdit) "Update product details" else "Product details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!isEdit) {
                Button(onClick = { showIntelligentCapture = true }, modifier = Modifier.fillMaxWidth()) { Text("INTELLIGENTLY IDENTIFY PRODUCT", fontWeight = FontWeight.Bold) }
                Text("Point the camera at the front of the product. Identity is a suggestion and must be reviewed before saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            identificationStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            identificationConfidence?.let { val level = when { it >= 95 -> "HIGH"; it >= 80 -> "GOOD"; it >= 60 -> "MEDIUM"; it > 0 -> "LOW"; else -> "NONE" }; Text("IDENTIFICATION CONFIDENCE: $level ($it%)", color = if (it >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
            identificationExplanation?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedTextField(name, { name = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("Product name") }, singleLine = true)
            OutlinedTextField(brand, { brand = it }, Modifier.fillMaxWidth(), label = { Text("Brand") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(barcode, { barcode = it; errorMessage = null }, Modifier.weight(1f), label = { Text("Primary barcode / GTIN") }, singleLine = true)
                OutlinedButton(onClick = { showBarcodeScanner = true }, modifier = Modifier.padding(top = 8.dp)) { Text("SCAN") }
            }
            OutlinedTextField(sku, { sku = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("SKU / Item code") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField(mrp, { mrp = it }, Modifier.weight(1f), label = { Text("MRP") }, singleLine = true); OutlinedTextField(sellingPrice, { sellingPrice = it }, Modifier.weight(1f), label = { Text("Sale price") }, singleLine = true) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField(purchasePrice, { purchasePrice = it }, Modifier.weight(1f), label = { Text("Purchase price") }, singleLine = true); OutlinedTextField(value = stock, onValueChange = { if (!isEdit) stock = it }, modifier = Modifier.weight(1f), label = { Text(if (isEdit) "Current stock" else "Opening stock") }, singleLine = true, enabled = !isEdit) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField(unit, { unit = it }, Modifier.weight(1f), label = { Text("Unit") }, singleLine = true); OutlinedTextField(lowStockThreshold, { lowStockThreshold = it }, Modifier.weight(1f), label = { Text("Low-stock alert") }, singleLine = true) }
            captureObservation?.pack?.let { pack -> val compatibility = ProductPackCompatibility.classify(pack, unit); Text("OBSERVED PACK: ${pack.sourceText} • ${compatibility.explanation}", color = if (compatibility.compatibility == PackCompatibility.MISMATCH_REQUIRES_REVIEW) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
            localCandidateStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            localCandidates.take(3).forEach { candidate ->
                Column(Modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LOCAL PRODUCT MATCH", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(candidate.product.name, fontWeight = FontWeight.Bold)
                    if (candidate.product.brand.isNotBlank()) Text("Brand: ${candidate.product.brand}", style = MaterialTheme.typography.bodyMedium)
                    Text("Match: ${candidate.score}% • ${candidate.explanation}", style = MaterialTheme.typography.bodySmall)
                    Text("This product already exists in this store. Opening it avoids creating a duplicate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { applyLocalCandidate(candidate) }, modifier = Modifier.fillMaxWidth()) { Text("OPEN EXISTING PRODUCT") }
                }
            }
            catalogStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            catalogCandidate?.let { catalog ->
                Column(Modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CATALOG MATCH", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    catalog.name?.let { Text("Name: $it", style = MaterialTheme.typography.bodyMedium) }
                    catalog.brand?.let { Text("Brand: $it", style = MaterialTheme.typography.bodyMedium) }
                    catalog.quantity?.let { Text("Quantity: $it", style = MaterialTheme.typography.bodyMedium) }
                    catalog.category?.let { Text("Category: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("Barcode-backed candidate. Applying it never changes retailer price, purchase price, stock or SKU.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { catalog.name?.let { name = it }; catalog.brand?.let { brand = it }; catalog.quantity?.let { captureHint = "Catalog quantity: $it" }; catalog.category?.let { captureHint = "Catalog category: $it" }; identificationStatus = "IDENTIFICATION: CATALOG APPLIED"; identificationConfidence = ProductIdentificationRanking.score(ProductIdentificationSignals(barcodeDetected = true, catalogMatched = true, barcodeMatchesCatalog = true)).score; identificationExplanation = "Catalog candidate accepted by the retailer. Verify all fields before saving."; catalogStatus = "Catalog identity applied. Store-controlled fields remain unchanged."; catalogCandidate = null }, modifier = Modifier.weight(1f)) { Text("USE CATALOG") }
                        OutlinedButton(onClick = { catalogCandidate = null; identificationConfidence = ProductIdentificationRanking.score(ProductIdentificationSignals(barcodeDetected = barcode.isNotBlank(), printedTextDetected = name.isNotBlank() || brand.isNotBlank())).score; identificationExplanation = "Catalog suggestion dismissed. Camera/OCR details remain under retailer control."; catalogStatus = "Catalog suggestion dismissed." }, modifier = Modifier.weight(1f)) { Text("KEEP CAMERA") }
                    }
                }
            }
            captureHint?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            Text("ADDITIONAL BAR CODES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (productId == null) Text("Save the product first, then add additional barcodes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                barcodes.filter { !it.isPrimary }.forEach { code -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(code.value, fontWeight = FontWeight.Bold); Text(code.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = { viewModel.removeSecondaryBarcode(code.id) }) { Text("REMOVE") } } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(secondaryBarcode, { secondaryBarcode = it; secondaryBarcodeError = null }, Modifier.weight(1f), label = { Text("Secondary barcode") }, singleLine = true)
                    OutlinedButton(onClick = { viewModel.addSecondaryBarcode(productId, secondaryBarcode, "UNKNOWN") { result -> when (result) { BarcodeMutationResult.Success -> { secondaryBarcode = ""; secondaryBarcodeError = null }; BarcodeMutationResult.Duplicate -> secondaryBarcodeError = "That barcode is already assigned to a product in this store."; BarcodeMutationResult.Invalid -> secondaryBarcodeError = "Enter a barcode before adding it." } } }, modifier = Modifier.padding(top = 8.dp)) { Text("ADD") }
                }
                secondaryBarcodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Text("Sale price cannot exceed MRP. Stock is managed separately after product creation. QR codes are not accepted as product identifiers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = ::persistProduct, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentPadding = PaddingValues(vertical = 16.dp)) { Text(if (isEdit) "SAVE CHANGES" else "SAVE PRODUCT", fontWeight = FontWeight.Bold) }
        }
    }
}
