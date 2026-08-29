package com.retailpos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.SuppressLint
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

@SuppressLint("UnsafeOptInUsageError")
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
                                    mainHandler.post {
                                        resultPreview = ProductCaptureResult(
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
                                        if (stability.stable) onResult(resultPreview!!)
                                    }
                                }
                            }

                            scanner.process(image).addOnSuccessListener { barcodes ->
                                barcodeRef.set(barcodes.firstOrNull { it.format != Barcode.FORMAT_QR_CODE }?.rawValue?.trim())
                            }.addOnCompleteListener { completed.incrementAndGet(); recordObservation() }

                            recognizer.process(image).addOnSuccessListener { text ->
                                textRef.set(text.text)
                            }.addOnCompleteListener { completed.incrementAndGet(); recordObservation() }

                            labeler.process(image).addOnSuccessListener { labels ->
                                labels.maxByOrNull { it.confidence }?.let {
                                    labelRef.set(it.text)
                                    labelConfidenceRef.set(it.confidence)
                                }
                            }.addOnCompleteListener { completed.incrementAndGet(); recordObservation() }
                        }
                    }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }

            providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

            onDispose {
                runCatching { providerFuture.get().unbindAll() }
                scanner.close()
                recognizer.close()
                labeler.close()
                mainHandler.removeCallbacksAndMessages(null)
            }
        }

        Column(Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().weight(1f))
            Surface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val preview = resultPreview
                    Text(preview?.detectedName ?: "Point the camera at a product", fontWeight = FontWeight.Bold)
                    Text(preview?.stabilityExplanation ?: "Hold steady while RetailGPT reads the barcode and packaging.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
