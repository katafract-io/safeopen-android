package com.katafract.safeopen.ui

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.katafract.safeopen.ui.components.ScanRadar
import com.katafract.safeopen.ui.theme.KataGold
import com.katafract.safeopen.ui.theme.SafeOpenHaptics
import com.katafract.safeopen.viewmodel.MainViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "ScannerScreen"

private enum class CamPermState { Unknown, Granted, Denied, PermanentlyDenied }

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    var permState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
            ) CamPermState.Granted else CamPermState.Unknown,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val tick = SafeOpenHaptics.rememberTickProvider()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permState = when {
            granted -> CamPermState.Granted
            activity != null && activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
                CamPermState.Denied
            else -> CamPermState.PermanentlyDenied
        }
    }

    // Trigger permission request on first composition if state is Unknown.
    LaunchedEffect(Unit) {
        if (permState == CamPermState.Unknown) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Bezel-to-bezel scanner preview: contentWindowInsets = 0 lets the camera
    // extend behind status/nav bars. Per-control insets are applied below
    // (top scrim + bottom action bar each consume systemBars).
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Black,
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (permState) {
                CamPermState.Granted -> CameraPreview(
                    viewModel = viewModel,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> PermissionGate(
                    state = permState,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Center reticle (gold corner ticks, decorative) — only when granted.
            if (permState == CamPermState.Granted) {
                ScanReticle(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp),
                )
            }

            // Top scrim (system-bar safe area + instruction copy + history FAB + Free pill)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                    )
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!isPro) {
                        BrandPill(
                            label = "Free",
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            tick()
                            onNavigateToHistory()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.92f),
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Point camera at QR code",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }

            // Bottom action bar — own systemBars inset for nav bar.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScanRadar(size = 56.dp)
                        Text(
                            text = "Inspecting…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(
                            onClick = {
                                tick()
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        viewModel.inspectPasted(text)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 6.dp),
                            )
                            Text(
                                text = "Paste link from clipboard",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        if (!isPro) {
                            Text(
                                text = "Free version · last 10 scans kept",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandPill(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = KataGold.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = KataGold,
        )
    }
}

@Composable
private fun ScanReticle(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Four corner ticks — purely decorative gold accents.
        val cornerSize = 28.dp
        val strokeWidth = 2.dp
        val color = KataGold.copy(alpha = 0.95f)

        // top-left
        Box(modifier = Modifier.align(Alignment.TopStart).size(cornerSize, strokeWidth).background(color))
        Box(modifier = Modifier.align(Alignment.TopStart).size(strokeWidth, cornerSize).background(color))
        // top-right
        Box(modifier = Modifier.align(Alignment.TopEnd).size(cornerSize, strokeWidth).background(color))
        Box(modifier = Modifier.align(Alignment.TopEnd).size(strokeWidth, cornerSize).background(color))
        // bottom-left
        Box(modifier = Modifier.align(Alignment.BottomStart).size(cornerSize, strokeWidth).background(color))
        Box(modifier = Modifier.align(Alignment.BottomStart).size(strokeWidth, cornerSize).background(color))
        // bottom-right
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(cornerSize, strokeWidth).background(color))
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(strokeWidth, cornerSize).background(color))
    }
}

@Composable
private fun PermissionGate(
    state: CamPermState,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera access needed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "SafeOpen needs your camera to scan QR codes for link safety inspection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        when (state) {
            CamPermState.PermanentlyDenied -> {
                OutlinedButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Open App Settings")
                }
            }
            else -> {
                Button(onClick = onRequest) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Grant Camera Access")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    viewModel: MainViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try { barcodeScanner.close() } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(imageProxy, barcodeScanner, viewModel)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    viewModel: MainViewModel,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    if (!viewModel.scannerActive.value) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val raw = barcode.rawValue ?: continue
                if (viewModel.scannerActive.value) {
                    viewModel.onQrScanned(raw)
                    break
                }
            }
        }
        .addOnFailureListener { e ->
            Log.w(TAG, "Barcode detect failed", e)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
