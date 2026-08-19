package com.retailpos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

data class ProductCaptureResult(
    val barcode: String?,
    val detectedName: String?,
    val detectedBrand: String?,
    val categoryHint: String?,
    val detectedMrp: Double?,
    val rawText: String,
    val labelConfidence: Float?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelligentProductCaptureScreen(
    onBack: () -> Unit,
    onResult: (ProductCaptureResult) -> Unit
) {
    val context = LocalContext.current
    var ready by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var resultPreview by remember { mutableStateOf<ProductCaptureResult?>(null) }
    var lastFingerprint by remember { mutableStateOf<String?>(null) }
    var lastEmitAt by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> ready = granted }

    LaunchedEffect(Unit) {
        if (!ready) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("INTELLIGENT PRODUCT CAPTURE") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!ready) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("ALLOW CAMERA") }
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = {
                    PreviewView(it).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        val scanner = BarcodeScanning.getClient()
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                        val executor = Executors.newSingleThreadExecutor()

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { useCase ->
                                useCase.setAnalyzer(executor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage == null) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    var barcodesDone = false
                                    var textDone = false
                                    var labelsDone = false
                                    var barcode: String? = null
                                    var text = ""
                                    var bestLabel: String? = null
                                    var bestConfidence: Float? = null

                                    fun maybeEmit() {
                                        if (!barcodesDone || !textDone || !labelsDone) return
                                        val parsed = parseCapture(text, bestLabel, bestConfidence, barcode)
                                        val fingerprint = listOf(parsed.barcode, parsed.detectedName, parsed.detectedBrand, parsed.categoryHint, parsed.detectedMrp).joinToString("|")
                                        val now = System.currentTimeMillis()
                                        if (fingerprint != lastFingerprint || now - lastEmitAt > 1_500L) {
                                            lastFingerprint = fingerprint
                                            lastEmitAt = now
                                            resultPreview = parsed
                                        }
                                    }

                                    scanner.process(image).addOnSuccessListener { codes ->
                                        barcode = codes.firstOrNull {
                                            it.format != Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrBlank()
                                        }?.rawValue
                                    }.addOnCompleteListener {
                                        barcodesDone = true
                                        maybeEmit()
                                    }
                                    recognizer.process(image).addOnSuccessListener { recognized ->
                                        text = recognized.text.orEmpty()
                                    }.addOnCompleteListener {
                                        textDone = true
                                        maybeEmit()
                                    }
                                    labeler.process(image).addOnSuccessListener { labels ->
                                        val top = labels.maxByOrNull { it.confidence }
                                        bestLabel = top?.text
                                        bestConfidence = top?.confidence
                                    }.addOnCompleteListener {
                                        labelsDone = true
                                        maybeEmit()
                                    }
                                    imageProxy.close()
                                }
                            }

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                context as androidx.lifecycle.LifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            Surface(Modifier.fillMaxWidth().padding(16.dp), tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Point the camera at the front of the product", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "We combine barcode + printed text + visual category hints. The result is a suggestion and must be reviewed before saving.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    resultPreview?.let { result ->
                        Spacer(Modifier.height(10.dp))
                        Text("Detected: ${result.detectedName ?: result.categoryHint ?: "Unknown product"}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        result.detectedBrand?.let { Text("Brand: $it", style = MaterialTheme.typography.bodySmall) }
                        result.barcode?.let { Text("Barcode: $it", style = MaterialTheme.typography.bodySmall) }
                        result.detectedMrp?.let { Text("Printed MRP: ₹${formatMoney(it)}", style = MaterialTheme.typography.bodySmall) }
                        result.categoryHint?.let { Text("Category hint: $it", style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onResult(result) }, modifier = Modifier.fillMaxWidth()) { Text("USE DETECTED DETAILS") }
                    }
                }
            }
        }
    }
}

private fun parseCapture(rawText: String, categoryHint: String?, labelConfidence: Float?, barcode: String?): ProductCaptureResult {
    val lines = rawText.lines().map { it.trim() }.filter { it.length >= 2 }
    val name = lines
        .filterNot { it.matches(Regex("[0-9 .₹$€£,/:-]+")) }
        .sortedWith(compareByDescending<String> { it.length }.thenBy { it.any { c -> c.isDigit() } })
        .firstOrNull()
    val metadataPattern = Regex("mrp|mfd|exp|net wt|qty|price|rs\\.?", RegexOption.IGNORE_CASE)
    val brand = lines.firstOrNull { line ->
        name != null && line != name && line.length in 2..40 && !metadataPattern.containsMatchIn(line)
    }
    val mrp = Regex("(?:MRP|M\\.R\\.P)[^0-9]{0,8}(?:₹|Rs\\.?|INR)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        .find(rawText.replace("\n", " "))
        ?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    return ProductCaptureResult(
        barcode = barcode,
        detectedName = name,
        detectedBrand = brand,
        categoryHint = categoryHint,
        detectedMrp = mrp,
        rawText = rawText,
        labelConfidence = labelConfidence
    )
}

private fun formatMoney(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
