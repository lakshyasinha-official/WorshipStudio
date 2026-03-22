package com.example.worshipstudio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size as CameraSize
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSessionScreen(
    churchId:        String,
    sessionViewModel: SessionViewModel,
    onJoined:        (sessionId: String) -> Unit,
    onBack:          () -> Unit
) {
    val state         by sessionViewModel.state.collectAsState()
    var codeInput     by remember { mutableStateOf("") }
    var showScanner   by remember { mutableStateOf(false) }
    var scannedOnce   by remember { mutableStateOf(false) }   // prevent double-fire

    // Start auto-discovery
    LaunchedEffect(churchId) {
        sessionViewModel.observeChurchSession(churchId)
    }

    // Pulsing animation for the discovery banner
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Join Session") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Auto-discovery banner ──────────────────────────────────────
                val discovered = state.churchActiveSession
                AnimatedVisibility(
                    visible = discovered != null,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    if (discovered != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    sessionViewModel.joinSession(discovered.sessionId, "")
                                    onJoined(discovered.sessionId)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1B5E20).copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Canvas(Modifier.size(14.dp)) {
                                    drawCircle(Color(0xFF4CAF50).copy(alpha = pulse))
                                    drawCircle(Color(0xFF4CAF50), radius = size.minDimension * 0.35f)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Live session in progress",
                                        fontWeight = FontWeight.Bold,
                                        color      = Color(0xFF1B5E20),
                                        style      = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "Tap to join instantly — no code needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Text(
                                    "JOIN →",
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = Color(0xFF1B5E20),
                                    fontSize   = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // ── Section divider ────────────────────────────────────────────
                Text(
                    "Or enter the 4-digit room code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                // ── 4-digit PIN display ────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        val digit = codeInput.getOrNull(i)?.toString() ?: ""
                        val isCurrent = i == codeInput.length
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .then(
                                    if (isCurrent)
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(12.dp)
                                        )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = digit,
                                fontSize   = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Hidden text field drives the PIN display ───────────────────
                OutlinedTextField(
                    value         = codeInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) codeInput = it
                    },
                    label         = { Text("Room code") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier      = Modifier.fillMaxWidth()
                )

                // Error message
                state.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))

                // ── Join by code button ────────────────────────────────────────
                Button(
                    onClick  = {
                        if (codeInput.length == 4) {
                            sessionViewModel.joinByCode(
                                code      = codeInput,
                                onSuccess = { sessionId ->
                                    sessionViewModel.joinSession(sessionId, "")
                                    onJoined(sessionId)
                                },
                                onError   = {}
                            )
                        }
                    },
                    enabled  = codeInput.length == 4 && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isLoading) "Joining…" else "Join Session")
                }

                Spacer(Modifier.height(16.dp))

                // ── QR scan button ─────────────────────────────────────────────
                Button(
                    onClick  = { showScanner = true; scannedOnce = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan QR Code")
                }
            }
        }

        // ── Full-screen QR scanner overlay ────────────────────────────────────
        AnimatedVisibility(
            visible  = showScanner,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            QrScannerOverlay(
                onCodeScanned = { rawValue ->
                    if (!scannedOnce) {
                        scannedOnce = true
                        // Extract 4-digit code from "worshipsync://join/{code}" or bare "1234"
                        val code = rawValue.removePrefix("worshipsync://join/").trim()
                        showScanner = false
                        sessionViewModel.joinByCode(
                            code      = code,
                            onSuccess = { sessionId ->
                                sessionViewModel.joinSession(sessionId, "")
                                onJoined(sessionId)
                            },
                            onError   = { scannedOnce = false }
                        )
                    }
                },
                onClose = { showScanner = false }
            )
        }
    }
}

// ── CameraX + MLKit QR scanner overlay ────────────────────────────────────────
@Composable
private fun QrScannerOverlay(
    onCodeScanned: (String) -> Unit,
    onClose:       () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraFuture.addListener({
                        val provider = cameraFuture.get()
                        val preview  = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val options  = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val scanner  = BarcodeScanning.getClient(options)
                        val resolution = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    CameraSize(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            ).build()
                        val analysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolution)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { imageProxy ->
                            @androidx.camera.core.ExperimentalGetImage
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { onCodeScanned(it) }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scan-frame overlay
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                )
                Text(
                    "Point at the admin's QR code",
                    color     = Color.White,
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                )
            }
        } else {
            // No permission
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission required", color = Color.White,
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // Close button
        IconButton(
            onClick  = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "Close scanner", tint = Color.White,
                modifier = Modifier.size(28.dp))
        }
    }
}
