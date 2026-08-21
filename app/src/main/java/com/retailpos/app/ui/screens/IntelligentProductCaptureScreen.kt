package com.retailpos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.retailpos.app.core.products.ProductCaptureConsensus
import com.retailpos.app.core.products.ProductCaptureObservation
import com.retailpos.app.core.products.ProductCaptureParser
import com.retailpos.app.core.products.ProductCaptureStabilityRules
import com.retailpos.app.core.products.ProductPackParser
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

data class ProductCaptureResult(
    val barcode: String?,
    val detectedName: String?,
    val detectedBrand: String?,
    val categoryHint: String?,
    val detectedMrp: Double?,
    val detectedPackSize: Double?,
    val detectedPackUnit: String?,
    val rawText: String,
    val labelConfidence: Float?,
    val frameCount: Int,
    val stabilityExplanation: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun IntelligentProductCaptureScreen(
    onBack: () -> Unit,
    onResult: (ProductCaptureResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var ready by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var resultPreview by remember { mutableStateOf<ProductCaptureResult?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> ready = granted }

    LaunchedEffect(Unit) {
        if (!ready) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("INTELLIGENT PRODUCT CAPTURE") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (!ready) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("ALLOW CAMERA") }
            }
            return@Scaffold
        }

        val previewView = remember(lifecycleOwner) { PreviewView(context) }
        val executor = remember(lifecycleOwner) { Executors.newSingleThreadExecutor() }

        DisposableEffect(lifecycleOwner, ready) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val scanner = BarcodeScanning.getClient()
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val mainHandler = Handler(Looper.getMainLooper())
            val observationRef = AtomicReference<List<ProductCaptureObservation>>(emptyList())
            val resultFingerprintRef = AtomicReference<String?>(null)

            val listener = Runnable {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
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
                            val barcodeRef = AtomicReference<String?>(null)
                            val textRef = AtomicReference("")
                            val labelRef = AtomicReference<String?>(null)
                            val labelConfidenceRef = AtomicReference<Float?>(null)
                            val completed = AtomicInteger(0)
                            val closed = AtomicReference(false)

                            fun closeOnce() {
                                if (closed.compareAndSet(false, true)) imageProxy.close()
                            }

                            fun recordObservation() {
                                if (completed.get() != 3) return
                                val text = textRef.get()
                                val parsed = ProductCaptureParser.parse(text)
                                val observation = ProductCaptureObservation(
                                    barcode = barcodeRef.get(),
                                    printedName = parsed.name,
                                    printedBrand = parsed.brand,
                                    mrp = parsed.mrp,
                                    categoryHint = labelRef.get(),
                                    categoryConfidence = labelConfidenceRef.get(),
                                    pack = ProductPackParser.parse(text),
                                    frameCount = 1
                                )
                                val next = (observationRef.get() + observation).takeLast(6)
                                observationRef.set(next)
                                val consensus = ProductCaptureConsensus.merge(next) ?: observation
                                val stability = ProductCaptureStabilityRules.evaluate(consensus)
                                val fingerprint = listOf(
                                    consensus.barcode,
                                    consensus.printedName,
                                    consensus.printedBrand,
                                    consensus.categoryHint,
                                    consensus.mrp,
                                    consensus.pack?.size,
                                    consensus.pack?.unit,
                                    consensus.frameCount,
                                    stability.stable
                                ).joinToString("|")
                                if (fingerprint != resultFingerprintRef.get()) {
                                    resultFingerprintRef.set(fingerprint)
                                    val nextResult = ProductCaptureResult(
                                        barcode = consensus.barcode,
                                        detectedName = consensus.printedName,
                                        detectedBrand = consensus.printedBrand,
                                        categoryHint = consensus.categoryHint,
                                        detectedMrp = consensus.mrp,
                                        detectedPackSize = consensus.pack?.size,
                                        detectedPackUnit = consensus.pack?.unit,
                                        rawText = text,
                                        labelConfidence = consensus.categoryConfidence,
                                        frameCount = consensus.frameCount,
                                        stabilityExplanation = stability.explanation
                                    )
                                    mainHandler.post { resultPreview = nextResult }
                                }
                                closeOnce()
                            }

                            fun markComplete() {
                                completed.incrementAndGet()
                                recordObservation()
                            }

                            scanner.process(image)
                                .addOnSuccessListener { codes ->
                                    barcodeRef.set(codes.firstOrNull { it.format != Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrBlank() }?.rawValue)
                                }
                                .addOnCompleteListener { markComplete() }

                            recognizer.process(image)
                                .addOnSuccessListener { recognized -> textRef.set(recognized.text.orEmpty()) }
                                .addOnCompleteListener { markComplete() }

                            labeler.process(image)
                                .addOnSuccessListener { labels ->
                                    val top = labels.maxByOrNull { it.confidence }
                                    labelRef.set(top?.text)
                                    labelConfidenceRef.set(top?.confidence)
                                }
                                .addOnCompleteListener { markComplete() }
                        }
                    }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (_: Exception) { }
            }

            providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
            onDispose {
                try { providerFuture.get().unbindAll() } catch (_: Exception) { }
                scanner.close()
                recognizer.close()
                labeler.close()
                executor.shutdownNow()
                mainHandler.removeCallbacksAndMessages(null)
            }
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = { previewView }, modifier = Modifier.weight(1f).fillMaxWidth())
            Surface(Modifier.fillMaxWidth().padding(16.dp), tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Point the camera at the front of the product", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("The app samples multiple frames and keeps only consistent evidence. Identity remains a suggestion until review.", style = MaterialTheme.typography.bodySmall)
                    resultPreview?.let { result ->
                        Spacer(Modifier.height(10.dp))
                        Text("Detected: ${result.detectedName ?: result.categoryHint ?: "Unknown product"}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        result.detectedBrand?.let { Text("Brand: $it", style = MaterialTheme.typography.bodySmall) }
                        result.barcode?.let { Text("Barcode: $it", style = MaterialTheme.typography.bodySmall) }
                        result.detectedMrp?.let { Text("Printed MRP: ₹${formatMoney(it)}", style = MaterialTheme.typography.bodySmall) }
                        if (result.detectedPackSize != null && !result.detectedPackUnit.isNullOrBlank()) Text("Pack size: ${formatQuantity(result.detectedPackSize)} ${result.detectedPackUnit}", style = MaterialTheme.typography.bodySmall)
                        result.categoryHint?.let { Text("Category hint: $it", style = MaterialTheme.typography.bodySmall) }
                        Text("Evidence frames: ${result.frameCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.stabilityExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onResult(result) },
                            enabled = ProductCaptureStabilityRules.evaluate(
                                ProductCaptureObservation(
                                    barcode = result.barcode,
                                    printedName = result.detectedName,
                                    printedBrand = result.detectedBrand,
                                    categoryHint = result.categoryHint,
                                    frameCount = result.frameCount
                                )
                            ).stable,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("USE DETECTED DETAILS") }
                    }
                }
            }
        }
    }
}

private fun formatMoney(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
