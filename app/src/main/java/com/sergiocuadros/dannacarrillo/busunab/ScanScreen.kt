package com.sergiocuadros.dannacarrillo.busunab

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sergiocuadros.dannacarrillo.busunab.repository.FaceRecognitionService
import com.sergiocuadros.dannacarrillo.busunab.repository.LogRepository
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@ComposePreview
@Composable
fun ScanScreen(
    userName: String = "Conductor1",
    onBack: () -> Unit = {},
    onBusView: () -> Unit = {}
) {
    // Modes: 0 = Facial, 1 = QR
    var selectedMode by remember { mutableIntStateOf(0) }
    val overlayIcon = if (selectedMode == 0) R.drawable.icon_user else R.drawable.icon_qr
    val overlayDesc = if (selectedMode == 0) "Reconocimiento Facial" else "QR"

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraError = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopNavigationBar(
                headerTitle = "Registro de estudiantes",
                userName = userName
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_log_out),
                        label = "Volver al bus",
                        modifier = Modifier.clickable { onBack() },
                        onClick = onBusView
                    ),
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_user),
                        label = "Reconocimiento Facial",
                        modifier = Modifier.clickable { selectedMode = 0 },
                        onClick = onBusView
                    )
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFB3E6FF)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Escanea",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(3f / 4f)
                    .border(width = 2.dp, color = Color.White)
                    .clickable(enabled = !isProcessing) {
                        if (selectedMode == 0 && !isProcessing) {
                            captureImage(context, imageCapture) { bitmap ->
                                capturedImage = bitmap
                                processImage(bitmap, context) { success, identity ->
                                    if (success) {
                                        LogRepository.registerLog(
                                            busId = userName,
                                            wasVerified = true,
                                            action = "face_verified"
                                        )
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted) {
                    if (cameraError.value == null) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .build()

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (exc: Exception) {
                                        cameraError.value =
                                            "Error al iniciar la cámara. Intenta reiniciar la app."
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        // Show error message
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0xFF5CA6C6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cameraError.value ?: "Error desconocido",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Request permission or show fallback
                    LaunchedEffect(Unit) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF5CA6C6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Se requiere permiso de cámara",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }

                Image(
                    painter = painterResource(id = overlayIcon),
                    contentDescription = overlayDesc,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture?,
    onImageCaptured: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)

    imageCapture?.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val bitmap = image.toBitmap()
                onImageCaptured(bitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("ScanScreen", "Error capturing image: ${exception.message}")
            }
        }
    )
}

private fun processImage(
    bitmap: Bitmap,
    context: Context,
    onResult: (Boolean, String?) -> Unit
) {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    val imageBytes = outputStream.toByteArray()

    FaceRecognitionService.verifyFace(imageBytes) { matched, identity, _ ->
        onResult(matched, identity)
    }
}

private fun androidx.camera.core.ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}