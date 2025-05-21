package com.sergiocuadros.dannacarrillo.busunab.repository

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FaceRecognitionService {
    private val client = OkHttpClient()

    fun verifyFace(
        imageBytes: ByteArray,
        onResult: (matched: Boolean, identity: String?, distance: Double?) -> Unit
    ) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "capture.jpg", imageBytes.toRequestBody())
            .build()

        val request = Request.Builder()
            .url("http://<TU_IP_EC2>/verify") // Reemplaza con la IP o dominio real
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 🔴 Error de red o tiempo de espera
                onResult(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        // 🔴 Código de error HTTP (500, 404, etc.)
                        onResult(false, null, null)
                        return
                    }

                    val jsonString = response.body?.string()
                    if (jsonString == null) {
                        onResult(false, null, null)
                        return
                    }

                    try {
                        val json = JSONObject(jsonString)

                        val matched = json.optBoolean("matched", false)
                        val identity = if (!json.isNull("identity")) json.getString("identity") else null
                        val distance = if (!json.isNull("distance")) json.getDouble("distance") else null

                        onResult(matched, identity, distance)
                    } catch (e: Exception) {
                        // 🔴 Error al parsear JSON (respuesta corrupta o no es JSON)
                        onResult(false, null, null)
                    }
                }
            }
        })
    }
}
