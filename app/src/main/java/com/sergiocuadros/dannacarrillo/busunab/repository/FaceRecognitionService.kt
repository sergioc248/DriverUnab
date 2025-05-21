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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "verifyFace onFailure", e)
                onResult(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "verifyFace HTTP ${it.code} ${it.message}")
                        onResult(false, null, null)
                        return
                    }

                    val jsonString = it.body?.string()
                    if (jsonString.isNullOrEmpty()) {
                        Log.e(TAG, "verifyFace empty body")
                        onResult(false, null, null)
                        return
                    }
                    Log.d(TAG, "verifyFace response: $jsonString")
                    try {
                        val json = JSONObject(jsonString)
                        val matched = json.optBoolean("matched", false)
                        val identity = json.optString("identity", null)
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "verifyFaceFile onFailure", e)
                onResult(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "verifyFaceFile HTTP ${it.code} ${it.message}")
                        onResult(false, null, null)
                        return
                    }

                    val jsonString = it.body?.string()
                    if (jsonString.isNullOrEmpty()) {
                        Log.e(TAG, "verifyFaceFile empty body")
                        onResult(false, null, null)
                        return
                    }
                    Log.d(TAG, "verifyFaceFile response: $jsonString")
                    try {
                        val json = JSONObject(jsonString)
                        val matched = json.optBoolean("matched", false)
                        val identity = json.optString("identity", null)
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "addFace onFailure", e)
                onResult(false, "Network error or server unavailable.")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val respBody = it.body?.string()
                    if (!it.isSuccessful || respBody == null) {
                        Log.e(TAG, "addFace error ${it.code}: $respBody")
                        onResult(false, "Server error: ${it.code}")
                        return
                    }
                    Log.d(TAG, "addFace response: $respBody")
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
}
