package com.example.homeserve.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URL

class CloudinaryRepository {

    companion object {
        // ⚠️ REPLACE THESE with your actual Cloudinary values from cloudinary.com dashboard
        const val CLOUD_NAME = "YOUR_CLOUD_NAME"         // e.g. "dxyz123abc"
        const val UPLOAD_PRESET = "YOUR_UPLOAD_PRESET"   // e.g. "homeserve_unsigned"
    }

    suspend fun uploadImage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Read image bytes from URI
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                val imageBytes = inputStream.readBytes()
                inputStream.close()

                val boundary = "----FormBoundary${System.currentTimeMillis()}"
                val uploadUrl = URL("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")

                val connection = uploadUrl.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                val outputStream = connection.outputStream
                val writer = PrintStream(outputStream)

                // -- upload_preset field
                writer.print("--$boundary\r\n")
                writer.print("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                writer.print("$UPLOAD_PRESET\r\n")

                // -- file field
                writer.print("--$boundary\r\n")
                writer.print("Content-Disposition: form-data; name=\"file\"; filename=\"upload.jpg\"\r\n")
                writer.print("Content-Type: image/jpeg\r\n\r\n")
                writer.flush()

                outputStream.write(imageBytes)
                outputStream.flush()

                writer.print("\r\n--$boundary--\r\n")
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    // Parse "secure_url" from JSON response
                    parseSecureUrl(response)
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText()
                    android.util.Log.e("Cloudinary", "Upload failed $responseCode: $errorBody")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseSecureUrl(json: String): String? {
        val key = "\"secure_url\":\""
        val start = json.indexOf(key)
        if (start == -1) return null
        val valueStart = start + key.length
        val valueEnd = json.indexOf("\"", valueStart)
        if (valueEnd == -1) return null
        return json.substring(valueStart, valueEnd).replace("\\/", "/")
    }
}
