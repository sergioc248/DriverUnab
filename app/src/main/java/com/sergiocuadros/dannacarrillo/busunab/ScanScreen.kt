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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.isGranted
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import java.util.concurrent.ExecutorService
import java.io.FileOutputStream
import java.io.InputStream
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }

    var isProcessing by remember { mutableStateOf(false) }
    var capturedPreviewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var verificationStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // States for Add Face mode
    var isAddFaceMode by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var personNameForAddFace by remember { mutableStateOf("") }
    var latestCapturedFileForAddFace: File? by remember { mutableStateOf(null) }

    // Request camera permission
    LaunchedEffect(key1 = cameraPermission.status) {
        if (!cameraPermission.status.isGranted && !cameraPermission.status.shouldShowRationale) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // Setup camera when permission is granted and lifecycle owner is available
    LaunchedEffect(key1 = cameraPermission.status, key2 = lifecycleOwner) {
        if (cameraPermission.status.isGranted) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val localImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                    .build()
                imageCapture = localImageCapture

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll() // Unbind previous use cases
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        localImageCapture
                    )
                    Log.d("ScanScreen", "CameraX Usecases bound to lifecycle")
                } catch (exc: Exception) {
                    Log.e("ScanScreen", "Use case binding failed", exc)
                    scope.launch { verificationStatus = "Error al iniciar cámara: ${exc.localizedMessage}" }
                }
            }, ContextCompat.getMainExecutor(context))
        } else {
            scope.launch { verificationStatus = "Permiso de cámara no concedido." }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            latestCapturedFileForAddFace?.delete() // Clean up if screen is disposed
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
                latestCapturedFileForAddFace?.delete() // Clear any pending file from camera
                latestCapturedFileForAddFace = null

                scope.launch {
                    // Try to copy URI to a temp file first
                    val tempFileFromGallery = withContext(Dispatchers.IO) {
                        copyUriToTempFile(context, it)
                    }

                    if (tempFileFromGallery != null) {
                        latestCapturedFileForAddFace = tempFileFromGallery
                        capturedPreviewBitmap = withContext(Dispatchers.IO) {
                            try {
                                val originalBitmap = BitmapFactory.decodeFile(tempFileFromGallery.absolutePath).also {
                                    Log.d("ScanScreen", "Bitmap loaded from gallery temp file: ${tempFileFromGallery.name}")
                                }
                                correctBitmapRotation(tempFileFromGallery.absolutePath, originalBitmap)
                            } catch (e: Exception) {
                                Log.e("ScanScreen", "Error decoding/rotating gallery temp file to bitmap", e)
                                tempFileFromGallery.delete() // Clean up if bitmap loading fails
                                latestCapturedFileForAddFace = null
                                null
                            }
                        }

                        if (capturedPreviewBitmap != null) {
                            if (isAddFaceMode) {
                                verificationStatus = "Imagen de galería lista. Ingresa el nombre."
                                showNameDialog = true
                                isProcessing = false
                            } else { // Verify mode with file
                                verificationStatus = "Verificando rostro de galería (archivo)..."
                                FaceRecognitionService.verifyFaceFile(tempFileFromGallery) { matched, identity, distance ->
                                    scope.launch(Dispatchers.Main) {
                                        if (matched) {
                                            val studentId = identity?.substringBeforeLast('.') ?: "Desconocida"
                                            verificationStatus = "¡Verificación exitosa: $studentId!"
                                            busViewModel.logStudentBoardedAndOccupySeat(busId, driverId, studentId, seatNumberToOccupy)
                                        } else {
                                            verificationStatus = "No se encontró coincidencia"
                                        }
                                        isProcessing = false
                                        tempFileFromGallery.delete()
                                        latestCapturedFileForAddFace = null // Clear after use
                                        Log.d("ScanScreen", "Gallery temp file deleted (verify mode): ${tempFileFromGallery.name}")
                                    }
                                }
                            }
                        } else { // Bitmap failed to load from temp file
                            verificationStatus = "Error al procesar imagen de galería."
                            isProcessing = false
                            // latestCapturedFileForAddFace was already nulled if bitmap loading failed
                        }

                    } else { // Failed to copy URI to temp file, fallback to Bitmap/Bytes for gallery
                        Log.w("ScanScreen", "Failed to copy gallery URI to temp file. Falling back to byte array.")
                        val bmp: android.graphics.Bitmap? = try {
                            withContext(Dispatchers.IO) {
                                if (Build.VERSION.SDK_INT < 28) {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                                } else {
                                    val source = ImageDecoder.createSource(context.contentResolver, it)
                                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                        decoder.isMutableRequired = true
                                    }
                                }.let { originalBitmap -> // Rotate bitmap from gallery if needed
                                    tempFileFromGallery?.absolutePath?.let { path -> // If we have a path
                                        correctBitmapRotation(path, originalBitmap)
                                    } ?: originalBitmap // else return original if no path
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ScanScreen", "Error loading bitmap from gallery (fallback)", e)
                            verificationStatus = "Error al cargar imagen de galería."
                            isProcessing = false
                            null
                        }

                        bmp?.let { loadedBitmap ->
                            capturedPreviewBitmap = loadedBitmap
                            if (isAddFaceMode) {
                                // For add face mode, if file copy failed, dialog will use bitmap to bytes
                                verificationStatus = "Imagen lista (sin archivo directo). Ingresa el nombre."
                                showNameDialog = true
                                isProcessing = false
                            } else { // Verify mode fallback with bytes
                                verificationStatus = "Verificando rostro de galería (bytes)..."
                                val bytes = ByteArrayOutputStream().use { out ->
                                    loadedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                    out.toByteArray()
                                }
                                Log.d("ScanScreen", "Gallery image size for verification (fallback bytes): ${bytes.size / 1024} KB")
                                FaceRecognitionService.verifyFace(bytes) { matched, identity, distance ->
                                    scope.launch(Dispatchers.Main) {
                                        if (matched) {
                                            val studentId = identity?.substringBeforeLast('.') ?: "Desconocida"
                                            verificationStatus = "¡Verificación exitosa: $studentId!"
                                            busViewModel.logStudentBoardedAndOccupySeat(busId, driverId, studentId, seatNumberToOccupy)
                                        } else {
                                            verificationStatus = "No se encontró coincidencia"
                                        }
                                        isProcessing = false
                                    }
                                }
                            }
                        } ?: run {
                            isProcessing = false // bmp is null from fallback path
                        }
                    }
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
                        onClick = {
                            latestCapturedFileForAddFace?.delete()
                            onBack()
                        }
                    )
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isAddFaceMode = !isAddFaceMode
                    verificationStatus = if (isAddFaceMode) "Modo Agregar Cara Activado" else "Modo Verificar Cara Activado"
                    capturedPreviewBitmap = null
                    latestCapturedFileForAddFace?.delete()
                    latestCapturedFileForAddFace = null
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
                .background(Color(0xFFB3E6FF)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if(isAddFaceMode) "Agregar Estudiante" else "Escanear Estudiante",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(3f / 4f)
                    .border(2.dp, Color.White)
                    .clickable(enabled = !isProcessing && cameraPermission.status.isGranted && imageCapture != null) {
                        if (!cameraPermission.status.isGranted) {
                            verificationStatus = "Se requiere permiso de cámara."
                            cameraPermission.launchPermissionRequest()
                            return@clickable
                        }
                        val localImageCapture = imageCapture
                        if (localImageCapture == null) {
                            verificationStatus = "Cámara no está lista."
                            return@clickable
                        }

                        isProcessing = true
                        verificationStatus = "Capturando imagen..."
                        capturedPreviewBitmap = null
                        latestCapturedFileForAddFace?.delete() // Delete previous if any
                        latestCapturedFileForAddFace = null

                        val photoFile: File = try {
                            File.createTempFile("camerax_temp_", ".jpg", context.cacheDir)
                        } catch (ex: IOException) {
                            Log.e("ScanScreen", "Error creating temp file for camera", ex)
                            verificationStatus = "Error al crear archivo temporal."
                            isProcessing = false
                            return@clickable
                        }

                        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        localImageCapture.takePicture(
                            outputFileOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch { // Switch to main for UI, IO for bitmap
                                        val bmp = withContext(Dispatchers.IO) {
                                            try {
                                                val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                                correctBitmapRotation(photoFile.absolutePath, originalBitmap)
                                            } catch (e: Exception) {
                                                Log.e("ScanScreen", "Error decoding/rotating captured file to bitmap", e)
                                                null
                                            }
                                        }

                                        if (bmp != null) {
                                            capturedPreviewBitmap = bmp
                                            if (isAddFaceMode) {
                                                verificationStatus = "Imagen capturada. Ingresa el nombre."
                                                latestCapturedFileForAddFace = photoFile // Keep file for dialog
                                                showNameDialog = true
                                                isProcessing = false // Dialog handles further processing
                                            } else { // Verify mode
                                                verificationStatus = "Verificando rostro (cámara)..."
                                                FaceRecognitionService.verifyFaceFile(photoFile) { matched, identity, distance ->
                                                    scope.launch(Dispatchers.Main) {
                                                        if (matched) {
                                                            val studentId = identity?.substringBeforeLast('.') ?: "Desconocida"
                                                            verificationStatus = "¡Verificación exitosa: $studentId!"
                                                            busViewModel.logStudentBoardedAndOccupySeat(busId, driverId, studentId, seatNumberToOccupy)
                                                        } else {
                                                            verificationStatus = "No se encontró coincidencia"
                                                        }
                                                        isProcessing = false
                                                        photoFile.delete() // Delete after verification
                                                        Log.d("ScanScreen", "Temp file deleted (verify mode): ${photoFile.name}")
                                                    }
                                                }
                                            }
                                        } else { // bmp is null
                                            verificationStatus = "Error al procesar imagen capturada."
                                            isProcessing = false
                                            photoFile.delete()
                                            Log.d("ScanScreen", "Temp file deleted (bitmap processing error): ${photoFile.name}")
                                        }
                                    }
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Log.e("ScanScreen", "Photo capture failed: ${exc.message}", exc)
                                    scope.launch(Dispatchers.Main) {
                                        verificationStatus = "Error al capturar: ${exc.localizedMessage}"
                                        isProcessing = false
                                        photoFile.delete() // Delete on capture error
                                        Log.d("ScanScreen", "Temp file deleted (capture error): ${photoFile.name}")
                                    }
                                }
                            }
                        )
                    }
            ) {
                if (cameraPermission.status.isGranted) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.matchParentSize()
                    )
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
                            Text(verificationStatus ?: "Se requiere permiso de cámara.", color = Color.White, fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.matchParentSize()
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
                        it.startsWith("¡Verificación exitosa") -> Color.Black
                        it.startsWith("Error") || it.startsWith("No se encontró") -> Color.Red
                        else -> Color.DarkGray
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        } // End of Column
    } // End of Scaffold

    if (showNameDialog && isAddFaceMode) { // Bitmap presence checked inside or before calling
        AlertDialog(
            onDismissRequest = {
                showNameDialog = false
                personNameForAddFace = ""
                isProcessing = false
                verificationStatus = "Adición cancelada."
                latestCapturedFileForAddFace?.delete()
                latestCapturedFileForAddFace = null
                Log.d("ScanScreen", "Add face dialog dismissed, temp file deleted if existed.")
            },
            title = { Text("Agregar Nueva Cara") },
            text = {
                Column {
                    capturedPreviewBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Imagen para agregar",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(it.width.toFloat() / it.height.toFloat())
                                .padding(bottom = 8.dp)
                                .border(1.dp, Color.Gray)
                        )
                    }
                    TextField(
                        value = personNameForAddFace,
                        onValueChange = { personNameForAddFace = it },
                        label = { Text("Nombre de la Persona") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (personNameForAddFace.isNotBlank()) {
                            isProcessing = true
                            verificationStatus = "Agregando cara: $personNameForAddFace..."

                            val imageFileToSubmit = latestCapturedFileForAddFace
                            if (imageFileToSubmit != null && imageFileToSubmit.exists()) {
                                // Case 1: Image from Camera (use File)
                                FaceRecognitionService.addFaceFile(imageFileToSubmit, personNameForAddFace) { success, message ->
                                    scope.launch(Dispatchers.Main) {
                                        verificationStatus = message ?: (if (success) "Cara '${personNameForAddFace}' agregada." else "Error al agregar cara.")
                                        isProcessing = false
                                        imageFileToSubmit.delete() // Delete after successful or failed add
                                        latestCapturedFileForAddFace = null
                                        Log.d("ScanScreen", "Temp file deleted after addFaceFile attempt: ${imageFileToSubmit.name}")
                                        if (success) {
                                            showNameDialog = false
                                            personNameForAddFace = ""
                                            isAddFaceMode = false // Switch mode
                                        }
                                    }
                                }
                            } else if (capturedPreviewBitmap != null) {
                                // Case 2: Image from Gallery (use Bitmap -> Bytes)
                                scope.launch { // IO for compression
                                    val imageBytes = withContext(Dispatchers.IO) {
                                        ByteArrayOutputStream().use { out ->
                                            capturedPreviewBitmap!!.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                            out.toByteArray()
                                        }
                                    }
                                    Log.d("ScanScreen", "AddFace (gallery): Compressed image size: ${imageBytes.size / 1024} KB")
                                    FaceRecognitionService.addFace(imageBytes, personNameForAddFace) { success, message ->
                                        scope.launch(Dispatchers.Main) {
                                            verificationStatus = message ?: (if (success) "Cara '${personNameForAddFace}' agregada." else "Error al agregar cara.")
                                            isProcessing = false
                                            if (success) {
                                                showNameDialog = false
                                                personNameForAddFace = ""
                                                isAddFaceMode = false // Switch mode
                                            }
                                        }
                                    }
                                }
                            } else {
                                verificationStatus = "No hay imagen para agregar."
                                isProcessing = false
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
                        isProcessing = false
                        verificationStatus = "Adición cancelada."
                        latestCapturedFileForAddFace?.delete()
                        latestCapturedFileForAddFace = null
                        Log.d("ScanScreen", "Add face dialog cancelled, temp file deleted if existed.")
                    }
                ) { Text("Cancelar") }
            }
        )
    }
}

private fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("gallery_temp_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
        inputStream?.use { input: InputStream ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        if (inputStream == null) {
            Log.e("copyUriToTempFile", "InputStream null for URI: $uri")
            tempFile.delete() // Clean up if input stream was null
            return null
        }
        Log.d("copyUriToTempFile", "Successfully copied URI to temp file: ${tempFile.absolutePath}")
        tempFile
    } catch (e: IOException) {
        Log.e("copyUriToTempFile", "Error copying URI to temp file", e)
        null
    }
}

private fun correctBitmapRotation(imagePath: String, bitmap: Bitmap): Bitmap {
    val exifInterface = ExifInterface(imagePath)
    val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1.0f, 1.0f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(-90f)
            matrix.preScale(-1.0f, 1.0f)
        }
        // Add other cases as needed, though these cover the most common rotations
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
