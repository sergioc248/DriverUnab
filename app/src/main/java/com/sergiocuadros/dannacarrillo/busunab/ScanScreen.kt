package com.sergiocuadros.dannacarrillo.busunab

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

@OptIn(ExperimentalPermissionsApi::class)
@ComposePreview
@Composable
fun ScanScreen(
    userName: String = "Conductor1",
    onBack: () -> Unit = {}
) {
    // Modes: 0 = Facial, 1 = QR
    var selectedMode by remember { mutableIntStateOf(0) }
    val overlayIcon = if (selectedMode == 0) R.drawable.icon_user else R.drawable.icon_qr
    val overlayDesc = if (selectedMode == 0) "Reconocimiento Facial" else "QR"

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraError = remember { mutableStateOf<String?>(null) }

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
                        modifier = Modifier.clickable { onBack() }
                    ),
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_user),
                        label = "Reconocimiento Facial",
                        modifier = Modifier.clickable { selectedMode = 0 }
                    ),
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_qr),
                        label = "QR",
                        modifier = Modifier.clickable { selectedMode = 1 }
                    )
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFB3E6FF)), // Light blue background
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
                    .border(width = 2.dp, color = Color.White),
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
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            ctx as androidx.lifecycle.LifecycleOwner,
                                            cameraSelector,
                                            preview
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
                // Overlay icon (user or qr)
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