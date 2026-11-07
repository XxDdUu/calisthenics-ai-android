package com.calisthenics.ai.ui.liveanalysis

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.LiveLandmark
import com.calisthenics.ai.ui.theme.LocalAppColors
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private data class JointConnection(val from: String, val to: String)

private val jointConnections = listOf(
    JointConnection("left_shoulder", "left_elbow"),
    JointConnection("left_elbow", "left_wrist"),
    JointConnection("left_shoulder", "left_hip"),
    JointConnection("left_hip", "left_knee"),
    JointConnection("left_knee", "left_ankle"),
    JointConnection("right_shoulder", "right_elbow"),
    JointConnection("right_elbow", "right_wrist"),
    JointConnection("right_shoulder", "right_hip"),
    JointConnection("right_hip", "right_knee"),
    JointConnection("right_knee", "right_ankle"),
    JointConnection("left_shoulder", "right_shoulder"),
    JointConnection("left_hip", "right_hip")
)

@Composable
fun LiveAnalysisScreen(
    exerciseCode: String,
    onBackClick: () -> Unit,
    viewModel: LiveAnalysisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(exerciseCode) {
        viewModel.setExercise(exerciseCode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!cameraPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera access is required for live analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow Camera")
                }
            }
            return@Box
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.showSkeleton) {
            PoseOverlay(
                landmarks = uiState.liveResult?.landmarks ?: emptyList(),
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }

                Row {
                    IconButton(
                        onClick = { viewModel.toggleFlash() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle flash"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(Icons.Default.SwitchCamera, contentDescription = "Switch camera")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "LIVE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(text = uiState.exerciseCode.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricChip(label = "Score", value = uiState.liveResult?.score?.let { "${it.roundToInt()}" } ?: "--")
                        MetricChip(label = "FPS", value = "5")
                        MetricChip(label = "Hold", value = uiState.liveResult?.holdDurationSec?.let { "${it.toInt()}s" } ?: "0s")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.error ?: (uiState.liveResult?.feedback?.firstOrNull()?.message ?: "Tracking body pose..."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { viewModel.toggleRunning() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isRunning) "Stop" else "Start")
                        }
                    }
                }
            }
        }

        if (previewView != null && cameraPermissionGranted) {
            CameraHost(
                previewView = previewView!!,
                exerciseCode = uiState.exerciseCode,
                isRunning = uiState.isRunning,
                useFrontCamera = uiState.usingFrontCamera,
                flashEnabled = uiState.flashEnabled,
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner,
                context = context
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PoseOverlay(
    landmarks: List<LiveLandmark>,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Canvas(modifier = modifier) {
        val size = size
        val valid = landmarks.filter { it.visible && it.confidence >= 0.38 }
        for (connection in jointConnections) {
            val a = valid.find { it.name == connection.from }
            val b = valid.find { it.name == connection.to }
            if (a != null && b != null) {
                drawLine(
                    color = appColors.success,
                    start = Offset((a.xNorm * size.width).toFloat(), (a.yNorm * size.height).toFloat()),
                    end = Offset((b.xNorm * size.width).toFloat(), (b.yNorm * size.height).toFloat()),
                    strokeWidth = 3f
                )
            }
        }
        valid.forEach { landmark ->
            drawCircle(
                color = appColors.info,
                radius = 6f,
                center = Offset((landmark.xNorm * size.width).toFloat(), (landmark.yNorm * size.height).toFloat()),
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun CameraHost(
    previewView: PreviewView,
    exerciseCode: String,
    isRunning: Boolean,
    useFrontCamera: Boolean,
    flashEnabled: Boolean,
    viewModel: LiveAnalysisViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: Context
) {
    val currentExerciseCode by rememberUpdatedState(exerciseCode)
    val currentViewModel by rememberUpdatedState(viewModel)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(analysisExecutor) {
        onDispose { analysisExecutor.shutdown() }
    }

    DisposableEffect(previewView, useFrontCamera, isRunning, flashEnabled, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var camera: Camera? = null

        val bindCamera = Runnable {
            try {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val useCases = mutableListOf<UseCase>(preview)
                if (isRunning) {
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val frameSequence = System.currentTimeMillis() / 1000L
                            val imageBytes = imageProxyToJpeg(imageProxy)
                            if (imageBytes != null) {
                                val tempFile = File.createTempFile("live-frame-", ".jpg")
                                tempFile.writeBytes(imageBytes)
                                currentViewModel.onFrameCaptured(
                                    imageFile = tempFile,
                                    frameSequence = frameSequence,
                                    timestampMs = System.currentTimeMillis(),
                                    sideView = true,
                                    exerciseCode = currentExerciseCode
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("LiveAnalysis", "Failed to capture camera frame", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                    useCases.add(analysis)
                }
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
                camera?.cameraControl?.enableTorch(flashEnabled && !useFrontCamera)
            } catch (e: Exception) {
                Log.e("LiveAnalysis", "Failed to bind camera", e)
            }
        }
        cameraProviderFuture.addListener(bindCamera, mainExecutor)

        onDispose {
            camera?.cameraControl?.enableTorch(false)
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            } else {
                cameraProviderFuture.addListener({
                    runCatching { cameraProviderFuture.get().unbindAll() }
                }, mainExecutor)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize())
}

private fun imageProxyToJpeg(imageProxy: ImageProxy): ByteArray? {
    return try {
        val yPlane = imageProxy.planes.getOrNull(0) ?: return null
        val uPlane = imageProxy.planes.getOrNull(1) ?: return null
        val vPlane = imageProxy.planes.getOrNull(2) ?: return null

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val output = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, output)
        output.toByteArray()
    } catch (e: Exception) {
        Log.e("LiveAnalysis", "Failed to convert image proxy to jpeg", e)
        null
    }
}
