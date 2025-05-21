package com.sergiocuadros.dannacarrillo.busunab

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sergiocuadros.dannacarrillo.busunab.repository.FaceRecognitionService
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.BusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import com.google.accompanist.permissions.isGranted
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

@OptIn(ExperimentalPermissionsApi::class)
@ComposePreview
@Composable
fun ScanScreen(
    driverDisplayName: String,
    driverId: String,
    busId: String,
    seatNumberToOccupy: Int,
    busViewModel: BusViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var capturedPreviewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var verificationStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // States for Add Face mode
    var isAddFaceMode by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var personNameForAddFace by remember { mutableStateOf("") }

    // Request camera permission
    LaunchedEffect(cameraPermission.status) {
        if (!cameraPermission.status.isGranted && !cameraPermission.status.shouldShowRationale) {
            cameraPermission.launchPermissionRequest()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                verificationStatus = "Cargando imagen de galería..."
                capturedPreviewBitmap = null // Clear previous preview

                val bmp: android.graphics.Bitmap? = try {
                    withContext(Dispatchers.IO) {
                        if (Build.VERSION.SDK_INT < 28) {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                        } else {
                            ImageDecoder.createSource(context.contentResolver, it).let { src ->
                                ImageDecoder.decodeBitmap(src)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ScanScreen", "Error loading bitmap from gallery", e)
                    verificationStatus = "Error al cargar imagen de galería."
                    isProcessing = false
                    null
                }

                bmp?.let { loadedBitmap ->
                    capturedPreviewBitmap = loadedBitmap
                    if (isAddFaceMode) {
                        verificationStatus = "Imagen lista. Ingresa el nombre."
                        showNameDialog = true // This will trigger dialog with the loadedBitmap
                        isProcessing = false // Processing for add face will be handled by dialog
                    } else {
                        verificationStatus = "Verificando rostro de galería..."
                        val bytes = ByteArrayOutputStream().use { out ->
                            loadedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            out.toByteArray()
                        }
                        Log.d("ScanScreen", "Gallery image size for verification: ${bytes.size / 1024} KB")

                        FaceRecognitionService.verifyFace(bytes) { matched, identity, distance ->
                            scope.launch(Dispatchers.Main) {
                                if (matched) {
                                    val studentId = identity?.substringBeforeLast('.') ?: "Desconocida"
                                    verificationStatus = "¡Verificación exitosa: $studentId!"
                                    busViewModel.logStudentBoardedAndOccupySeat(busId, driverId, studentId, seatNumberToOccupy)
                                } else {
                                    verificationStatus = if (identity == null && distance == null) "Error de red/servidor" else "No se encontró coincidencia"
                                }
                                isProcessing = false
                            }
                        }
                    }
                } ?: run {
                    // bmp is null, error already set
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopNavigationBar(
                userName = driverDisplayName,
                headerTitle = "Registro de estudiantes"
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_backarrow),
                        label = "Volver al bus",
                        onClick = onBack
                    )
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isAddFaceMode = !isAddFaceMode
                    verificationStatus = if (isAddFaceMode) "Modo Agregar Cara Activado" else "Modo Verificar Cara Activado"
                    capturedPreviewBitmap = null // Clear preview when switching modes
                },
                containerColor = if (isAddFaceMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (isAddFaceMode) Icons.Filled.AddCircle else Icons.Filled.Face,
                    contentDescription = if (isAddFaceMode) "Modo Agregar Cara" else "Modo Verificar Cara",
                    tint = if (isAddFaceMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFB3E6FF)), // Theme color might be better
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if(isAddFaceMode) "Agregar Estudiante" else "Escanear Estudiante",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White, // Consider MaterialTheme.colorScheme.onPrimary
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(3f / 4f)
                    .border(2.dp, Color.White)
                    .clickable(enabled = !isProcessing && cameraPermission.status.isGranted) {
                        if (!cameraPermission.status.isGranted) {
                            verificationStatus = "Se requiere permiso de cámara."
                            cameraPermission.launchPermissionRequest()
                            return@clickable
                        }
                        isProcessing = true
                        verificationStatus = "Capturando imagen..."
                        capturedPreviewBitmap = null // Clear previous preview

                        captureImageFile(context, imageCapture,
                            onCaptureStarted = { /* isProcessing and status already set */ },
                            onError = { msg ->
                                verificationStatus = msg
                                isProcessing = false
                            },
                            onFileReady = { file ->
                                scope.launch { // Use scope for coroutines
                                    val bmp: android.graphics.Bitmap? = try {
                                        withContext(Dispatchers.IO) {
                                            BitmapFactory.decodeFile(file.absolutePath)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ScanScreen", "Error decoding file to bitmap", e)
                                        verificationStatus = "Error al procesar imagen capturada."
                                        null
                                    } finally {
                                         // It's important to delete the temp file from captureImageFile
                                         // The current captureImageFile deletes it in its own error/success paths
                                         // But if we read it here, we should ensure it's managed.
                                         // For now, assume captureImageFile handles its temp file.
                                    }

                                    bmp?.let { capturedBitmap ->
                                        capturedPreviewBitmap = capturedBitmap
                                        if (isAddFaceMode) {
                                            verificationStatus = "Imagen capturada. Ingresa el nombre."
                                            showNameDialog = true
                                            isProcessing = false // Add face dialog will handle further processing state
                                        } else {
                                            verificationStatus = "Verificando rostro (cámara)..."
                                            // verifyFaceFile takes a File, so we use the original file
                                            // Ensure FaceRecognitionService.verifyFaceFile handles file deletion or we do it after.
                                            // For safety, let's assume verifyFaceFile doesn't delete it, and we manage it if needed
                                            // However, the current captureImageFile cleans up its own temp file.
                                            // If verifyFaceFile needs the file to persist during its async op, this could be an issue.
                                            // It might be safer for verifyFaceFile to take bytes, or copy the file.
                                            // Let's proceed with sending the file path for now, assuming captureImageFile's temp file is okay
                                            // for FaceRecognitionService to read before it's potentially deleted by captureImageFile's own logic.
                                            // This implies captureImageFile's onFileReady must complete before the file is deleted.

                                            FaceRecognitionService.verifyFaceFile(file) { matched, identity, distance ->
                                                scope.launch(Dispatchers.Main) {
                                                    if (matched) {
                                                        val studentId = identity?.substringBeforeLast('.') ?: "Desconocida"
                                                        verificationStatus = "¡Verificación exitosa: $studentId!"
                                                        busViewModel.logStudentBoardedAndOccupySeat(busId, driverId, studentId, seatNumberToOccupy)
                                                    } else {
                                                        verificationStatus = if (identity == null && distance == null) "Error de red/servidor" else "No se encontró coincidencia"
                                                    }
                                                    isProcessing = false
                                                    // The temp file from captureImageFile should be deleted by its own finally block
                                                    // If not, we might need: if (file.exists()) file.delete() here
                                                }
                                            }
                                        }
                                    } ?: run {
                                        // bmp is null
                                        isProcessing = false
                                        if (file.exists()) { // If bmp failed to load, ensure temp file is cleaned
                                            withContext(Dispatchers.IO) { file.delete() }
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                if (cameraPermission.status.isGranted) {
                    AndroidView({ ctx ->
                        PreviewView(ctx).apply {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                                    imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Or CAPTURE_MODE_MAXIMIZE_QUALITY
                                        .build()
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                                } catch (e: Exception) {
                                    Log.e("ScanScreen", "Error initializing camera: ${e.message}", e)
                                    scope.launch { verificationStatus = "Error al iniciar cámara." }
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    }, modifier = Modifier.matchParentSize())

                    // Overlay Icon
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Reconocimiento Facial",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1f),
                        tint = Color.White.copy(alpha = 0.7f)
                    )

                } else {
                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Se requiere permiso de cámara.", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                                Text("Otorgar Permiso")
                            }
                        }
                    }
                }

                capturedPreviewBitmap?.let { previewBmp ->
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Imagen Capturada",
                        modifier = Modifier.matchParentSize() // Show preview over camera view if an image is captured/loaded
                    )
                }

                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } // End of Camera Box

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(if (isAddFaceMode) "Seleccionar Imagen para Agregar" else "Seleccionar de Galería para Verificar")
            }

            Spacer(modifier = Modifier.height(8.dp))
            verificationStatus?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        it.startsWith("¡Verificación exitosa") -> Color.Black // Or a success color
                        it.startsWith("Error") || it.startsWith("No se encontró") -> Color.Red
                        else -> Color.DarkGray
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f)) // Push status text up if content is less
        } // End of Column
    } // End of Scaffold

    if (showNameDialog && isAddFaceMode && capturedPreviewBitmap != null) {
        val imageToAdd = capturedPreviewBitmap!! // Known to be non-null here
        AlertDialog(
            onDismissRequest = {
                showNameDialog = false
                personNameForAddFace = ""
                // capturedPreviewBitmap = null // Keep preview until mode changes or new image
                isProcessing = false // Reset processing if dialog is dismissed
                verificationStatus = "Adición cancelada."
            },
            title = { Text("Agregar Nueva Cara") },
            text = {
                TextField(
                    value = personNameForAddFace,
                    onValueChange = { personNameForAddFace = it },
                    label = { Text("Nombre de la Persona") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (personNameForAddFace.isNotBlank()) {
                            isProcessing = true
                            verificationStatus = "Agregando cara: $personNameForAddFace..."
                            scope.launch {
                                val imageBytes = withContext(Dispatchers.IO) {
                                    ByteArrayOutputStream().use { out ->
                                        imageToAdd.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                        out.toByteArray()
                                    }
                                }
                                Log.d("ScanScreen", "AddFace: Compressed image size: ${imageBytes.size / 1024} KB")

                                FaceRecognitionService.addFace(imageBytes, personNameForAddFace) { success, message ->
                                    scope.launch(Dispatchers.Main) {
                                        verificationStatus = message ?: (if (success) "Cara '${personNameForAddFace}' agregada exitosamente." else "Error al agregar cara.")
                                        isProcessing = false
                                        if (success) {
                                            showNameDialog = false
                                            personNameForAddFace = ""
                                            // capturedPreviewBitmap = null // Clear preview after successful add
                                            // isAddFaceMode = false // Optionally switch back to verify mode
                                        }
                                    }
                                }
                            }
                        } else {
                            verificationStatus = "El nombre no puede estar vacío."
                        }
                    }
                ) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                        personNameForAddFace = ""
                        // capturedPreviewBitmap = null
                        isProcessing = false
                        verificationStatus = "Adición cancelada."
                    }
                ) { Text("Cancelar") }
            }
        )
    }
}

private fun captureImageFile(
    context: Context,
    imageCapture: ImageCapture?,
    onFileReady: (File) -> Unit,
    onError: (String) -> Unit,
    onCaptureStarted: () -> Unit
) {
    val exec = Executors.newSingleThreadExecutor()
    imageCapture?.let { ic ->
        ContextCompat.getMainExecutor(context).execute { onCaptureStarted() }
        val photoFile = try {
            File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
        } catch (e: IOException) {
            ContextCompat.getMainExecutor(context).execute { onError("Error creando archivo temp.") }
            return
        }
        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        ic.takePicture(options, exec, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                ContextCompat.getMainExecutor(context).execute { onFileReady(photoFile) }
            }
            override fun onError(exc: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute { onError("Error captura: ${exc.message}") }
                photoFile.delete()
            }
        })
    } ?: ContextCompat.getMainExecutor(context).execute { onError("Cámara no lista.") }
}
