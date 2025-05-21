package com.sergiocuadros.dannacarrillo.busunab.utils

val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }

val outputDirectory = context.cacheDir // Carpeta temporal

fun takePhoto(
    busId: String,
    onResult: (File?) -> Unit
) {
    val file = File(outputDirectory, "capture_${System.currentTimeMillis()}.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.value?.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(file)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onResult(null)
            }
        }
    )
}
