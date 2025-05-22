package com.sergiocuadros.dannacarrillo.busunab.repository

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody.Part
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

object FaceRecognitionService {
    private const val TAG = "FaceRecognitionService"
    private val client = OkHttpClient()

    /**
     * Sends raw JPEG bytes as multipart/form-data "file" to /verify
     */
    fun verifyFace(
        imageBytes: ByteArray,
        onResult: (matched: Boolean, identity: String?, distance: Double?) -> Unit
    ) {
        Log.d(TAG, "verifyFace: Initiating request to /verify")
        Log.d(TAG, "verifyFace: Image bytes length: ${imageBytes.size}")
        val jpegMedia = "image/jpeg".toMediaTypeOrNull()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = "capture.jpg",
                body = imageBytes.toRequestBody(jpegMedia)
            )
            .build()

        val request = Request.Builder()
            .url("http://34.207.47.83:8000/verify")
            .post(body)
            .build()
        Log.d(TAG, "verifyFace: Request built: ${request.method} ${request.url}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "verifyFace onFailure", e)
                onResult(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "verifyFace HTTP ${it.code} ${it.message}")
                        Log.d(TAG, "verifyFace: Response body: ${it.body?.string()}")
                        onResult(false, null, null)
                        return
                    }

                    val jsonString = it.body?.string()
                    Log.d(TAG, "verifyFace: Raw JSON response: $jsonString")
                    if (jsonString.isNullOrEmpty()) {
                        Log.e(TAG, "verifyFace empty body")
                        onResult(false, null, null)
                        return
                    }
                    Log.d(TAG, "verifyFace response: $jsonString")
                    try {
                        val json = JSONObject(jsonString)
                        val matched = json.optBoolean("matched", false)
                        val identity = json.optString("identity", "null")
                        val distance = if (json.has("distance")) json.optDouble("distance") else null
                        onResult(matched, identity, distance)
                    } catch (e: Exception) {
                        Log.e(TAG, "verifyFace JSON parse error", e)
                        onResult(false, null, null)
                    }
                }
            }
        })
    }

    /**
     * Sends a File directly as multipart/form-data "file" to /verify
     */
    fun verifyFaceFile(
        file: File,
        onResult: (matched: Boolean, identity: String?, distance: Double?) -> Unit
    ) {
        Log.d(TAG, "verifyFaceFile: Initiating request to /verify")
        Log.d(TAG, "verifyFaceFile: File path: ${file.absolutePath}, File size: ${file.length()}")
        val jpegMedia = "image/jpeg".toMediaTypeOrNull()
        val part = Part.createFormData("file", file.name, file.asRequestBody(jpegMedia))
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(part)
            .build()

        val request = Request.Builder()
            .url("http://34.207.47.83:8000/verify")
            .post(body)
            .build()
        Log.d(TAG, "verifyFaceFile: Request built: ${request.method} ${request.url}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "verifyFaceFile onFailure", e)
                onResult(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "verifyFaceFile HTTP ${it.code} ${it.message}")
                        Log.d(TAG, "verifyFaceFile: Response body: ${it.body?.string()}")
                        onResult(false, null, null)
                        return
                    }

                    val jsonString = it.body?.string()
                    Log.d(TAG, "verifyFaceFile: Raw JSON response: $jsonString")
                    if (jsonString.isNullOrEmpty()) {
                        Log.e(TAG, "verifyFaceFile empty body")
                        onResult(false, null, null)
                        return
                    }
                    Log.d(TAG, "verifyFaceFile response: $jsonString")
                    try {
                        val json = JSONObject(jsonString)
                        val matched = json.optBoolean("matched", false)
                        val identity = json.optString("identity", "null")
                        val distance = if (json.has("distance")) json.optDouble("distance") else null
                        onResult(matched, identity, distance)
                    } catch (e: Exception) {
                        Log.e(TAG, "verifyFaceFile JSON parse error", e)
                        onResult(false, null, null)
                    }
                }
            }
        })
    }

    /**
     * Sends raw JPEG bytes plus a "name" field to /add-face
     */
    fun addFace(
        imageBytes: ByteArray,
        name: String,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        Log.d(TAG, "addFace: Initiating request to /add-face")
        Log.d(TAG, "addFace: Image bytes length: ${imageBytes.size}, Name: $name")
        val jpegMedia = "image/jpeg".toMediaTypeOrNull()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "new_face.jpg", imageBytes.toRequestBody(jpegMedia))
            .addFormDataPart("name", name)
            .build()

        val request = Request.Builder()
            .url("http://34.207.47.83:8000/add-face")
            .post(body)
            .build()
        Log.d(TAG, "addFace: Request built: ${request.method} ${request.url}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "addFace onFailure", e)
                onResult(false, "Network error or server unavailable.")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val respBody = it.body?.string()
                    Log.d(TAG, "addFace: Raw JSON response: $respBody")
                    if (!it.isSuccessful) {
                        var errorMessage = "Server error: ${it.code}"
                        if (respBody != null) {
                            try {
                                val errorJson = JSONObject(respBody)
                                if (errorJson.has("detail")) {
                                    val detailArray = errorJson.getJSONArray("detail")
                                    if (detailArray.length() > 0) {
                                        val firstError = detailArray.getJSONObject(0)
                                        val msg = firstError.optString("msg", "Unknown validation error")
                                        val locArray = firstError.optJSONArray("loc")
                                        var locString = ""
                                        if (locArray != null && locArray.length() > 0) {
                                            locString = " (field: ${locArray.optString(locArray.length() - 1)})"
                                        }
                                        errorMessage = "Validation error: $msg$locString - Code: ${it.code}"
                                    }
                                } else if (errorJson.has("error")) { // For general errors like verify's 500
                                     errorMessage = "Server error: ${errorJson.optString("error", respBody)} - Code: ${it.code}"
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "addFace: Could not parse error response body: $respBody", e)
                                // Keep generic error message if parsing fails but body is not null
                                errorMessage = "Server error: ${it.code} - Response: $respBody"
                            }
                        }
                        Log.e(TAG, "addFace error: $errorMessage")
                        onResult(false, errorMessage)
                        return
                    }
                    if (respBody == null) { // Should ideally be caught by !it.isSuccessful if server behaves
                        Log.e(TAG, "addFace error: Empty response body on successful call")
                        onResult(false, "Server returned empty response.")
                        return
                    }
                    try {
                        val json = JSONObject(respBody)
                        val message = json.optString("message", "Face added.")
                        onResult(true, message)
                    } catch (e: Exception) {
                        Log.e(TAG, "addFace JSON parse error", e)
                        onResult(false, "Error parsing server response.")
                    }
                }
            }
        })
    }

    /**
     * Sends a File plus a "name" field to /add-face
     */
    fun addFaceFile(
        file: File,
        name: String,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        Log.d(TAG, "addFaceFile: Initiating request to /add-face")
        Log.d(TAG, "addFaceFile: File path: ${file.absolutePath}, File size: ${file.length()}, Name: $name")

        val jpegMedia = "image/jpeg".toMediaTypeOrNull()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(jpegMedia))
            .addFormDataPart("name", name)
            .build()

        val request = Request.Builder()
            .url("http://34.207.47.83:8000/add-face")
            .post(body)
            .build()
        Log.d(TAG, "addFaceFile: Request built: ${request.method} ${request.url}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "addFaceFile onFailure", e)
                onResult(false, "Network error or server unavailable.")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val respBody = it.body?.string()
                    Log.d(TAG, "addFaceFile: Raw JSON response: $respBody")
                    if (!it.isSuccessful) {
                        var errorMessage = "Server error: ${it.code}"
                        if (respBody != null) {
                            try {
                                val errorJson = JSONObject(respBody)
                                if (errorJson.has("detail")) {
                                    val detailArray = errorJson.getJSONArray("detail")
                                    if (detailArray.length() > 0) {
                                        val firstError = detailArray.getJSONObject(0)
                                        val msg = firstError.optString("msg", "Unknown validation error")
                                        val locArray = firstError.optJSONArray("loc")
                                        var locString = ""
                                        if (locArray != null && locArray.length() > 0) {
                                            locString = " (field: ${locArray.optString(locArray.length() - 1)})"
                                        }
                                        errorMessage = "Validation error: $msg$locString - Code: ${it.code}"
                                    }
                                } else if (errorJson.has("error")) { // For general errors like verify's 500
                                     errorMessage = "Server error: ${errorJson.optString("error", respBody)} - Code: ${it.code}"
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "addFaceFile: Could not parse error response body: $respBody", e)
                                // Keep generic error message if parsing fails but body is not null
                                 errorMessage = "Server error: ${it.code} - Response: $respBody"
                            }
                        }
                        Log.e(TAG, "addFaceFile error: $errorMessage")
                        onResult(false, errorMessage)
                        return
                    }
                    if (respBody == null) { // Should ideally be caught by !it.isSuccessful if server behaves
                        Log.e(TAG, "addFaceFile error: Empty response body on successful call")
                        onResult(false, "Server returned empty response.")
                        return
                    }
                    try {
                        val json = JSONObject(respBody)
                        // Assuming the server responds with a "message" field on success,
                        // similar to the addFace endpoint.
                        val message = json.optString("message", "Face added successfully using file.")
                        onResult(true, message)
                    } catch (e: Exception) {
                        Log.e(TAG, "addFaceFile JSON parse error", e)
                        onResult(false, "Error parsing server response.")
                    }
                }
            }
        })
    }
}
