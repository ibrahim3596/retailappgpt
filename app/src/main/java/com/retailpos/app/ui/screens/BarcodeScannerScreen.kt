package com.retailpos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.retailpos.app.core.products.ProductBarcodeDecision
import com.retailpos.app.core.products.ProductBarcodeSafety
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private enum class ScannerState { REQUESTING_PERMISSION, READY, DENIED }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    title: String,
    onBack: () -> Unit,
    onBarcodeDetected: (rawValue: String, format: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannerState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                ScannerState.READY
            } else {
                ScannerState.REQUESTING_PERMISSION
            }
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var lastScan by remember { mutableStateOf<String?>(null) }
    var lastScanAt by remember { mutableLongStateOf(0L) }
    var ignoredScanMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scannerState = if (granted) ScannerState.READY else ScannerState.DENIED
    }

    LaunchedEffect(scannerState) {
        if (scannerState == ScannerState.REQUESTING_PERMISSION) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            when (scannerState) {
                ScannerState.READY -> {
                    val previewView = remember(lifecycleOwner) {
                        PreviewView(context).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    }

                    DisposableEffect(lifecycleOwner, torchEnabled) {
                        val providerFuture = ProcessCameraProvider.getInstance(context)
                        val scannerOptions = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E,
                                Barcode.FORMAT_ITF,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_CODABAR,
                                Barcode.FORMAT_DATA_MATRIX,
                                Barcode.FORMAT_PDF417,
                                Barcode.FORMAT_AZTEC
                            )
                            .build()
                        val scanner = BarcodeScanning.getClient(scannerOptions)
                        val executor: ExecutorService = Executors.newSingleThreadExecutor()
                        var cameraProvider: ProcessCameraProvider? = null

                        val listener = Runnable {
                            val provider = runCatching { providerFuture.get() }.getOrNull() ?: return@Runnable
                            cameraProvider = provider
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
                                        scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                                            .addOnSuccessListener { barcodes ->
                                                val hit = barcodes.firstOrNull {
                                                    !it.rawValue.isNullOrBlank() && it.format != Barcode.FORMAT_QR_CODE
                                                }
                                                val raw = hit?.rawValue?.trim()
                                                if (!raw.isNullOrBlank()) {
                                                    when (ProductBarcodeSafety.classify(raw)) {
                                                        ProductBarcodeDecision.ACCEPT -> {
                                                            val now = System.currentTimeMillis()
                                                            if (raw != lastScan || now - lastScanAt > 1_000L) {
                                                                lastScan = raw
                                                                lastScanAt = now
                                                                ignoredScanMessage = null
                                                                onBarcodeDetected(raw, hit.format)
                                                            }
                                                        }
                                                        ProductBarcodeDecision.IGNORE_QR -> {
                                                            ignoredScanMessage = "This looks like a QR/payment/link payload, not a product barcode."
                                                        }
                                                        ProductBarcodeDecision.REJECT_INVALID -> {
                                                            ignoredScanMessage = "That barcode value is not a valid retail product identifier."
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    }
                                }

                            runCatching { provider.unbindAll() }
                            runCatching {
                                val camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                                camera.cameraControl.enableTorch(torchEnabled)
                            }
                        }

                        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

                        onDispose {
                            runCatching { cameraProvider?.unbindAll() }
                            scanner.close()
                            executor.shutdownNow()
                        }
                    }

                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(280.dp, 180.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { torchEnabled = !torchEnabled }) {
                                Icon(Icons.Default.FlashOn, contentDescription = "Flash")
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Align product barcode inside the frame", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    ignoredScanMessage ?: "QR codes are not accepted in the product scanner",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (ignoredScanMessage != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                ScannerState.DENIED -> {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Camera access is required", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        Text("Allow camera access in Android settings to scan products.", color = Color.LightGray)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("ALLOW CAMERA") }
                    }
                }
                ScannerState.REQUESTING_PERMISSION -> Unit
            }
        }
    }
}
