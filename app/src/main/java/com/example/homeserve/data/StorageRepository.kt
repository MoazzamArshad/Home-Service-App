package com.example.homeserve.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class StorageRepository {

    /**
     * Uploads any file or image anonymously using stable public APIs.
     * Tries Catbox first (fast, permanent link, plain-text response).
     * If Catbox fails, falls back to Tmpfiles.org (JSON response).
     */
    suspend fun uploadFile(context: Context, uri: Uri, folderPath: String): String? {
        val uriString = uri.toString()

        // Already a network URL — no need to re-upload
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }

        // Not a local URI — return as-is
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return uriString
        }

        return withContext(Dispatchers.IO) {
            // Read file bytes and content resolver details
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: return@withContext null

            // Determine file extension
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("pdf") -> "pdf"
                mimeType.contains("png") -> "png"
                mimeType.contains("gif") -> "gif"
                mimeType.contains("word") || mimeType.contains("office") -> "docx"
                else -> "jpg"
            }
            val fileName = "upload_${UUID.randomUUID().toString().take(8)}.$extension"

            // 1. Try Catbox.moe upload
            val catboxUrl = tryCatboxUpload(bytes, fileName)
            if (catboxUrl != null) {
                return@withContext catboxUrl
            }

            // 2. Fallback to Tmpfiles.org upload
            val tmpfilesUrl = tryTmpfilesUpload(bytes, fileName)
            if (tmpfilesUrl != null) {
                return@withContext tmpfilesUrl
            }

            null
        }
    }

    private fun tryCatboxUpload(bytes: ByteArray, fileName: String): String? {
        return try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("https://catbox.moe/user/api.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            val request = conn.outputStream
            val writer = DataOutputStream(request)

            // Parameter: reqtype = fileupload
            writer.writeBytes("--$boundary\r\n")
            writer.writeBytes("Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n")
            writer.writeBytes("fileupload\r\n")

            // Parameter: fileToUpload = [bytes]
            writer.writeBytes("--$boundary\r\n")
            writer.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"$fileName\"\r\n\r\n")
            writer.write(bytes)
            writer.writeBytes("\r\n")

            // End boundary
            writer.writeBytes("--$boundary--\r\n")
            writer.flush()
            writer.close()

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText().trim()
                if (response.startsWith("https://") || response.startsWith("http://")) {
                    response
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tryTmpfilesUpload(bytes: ByteArray, fileName: String): String? {
        return try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("https://tmpfiles.org/api/v1/upload")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            val request = conn.outputStream
            val writer = DataOutputStream(request)

            // Parameter: file = [bytes]
            writer.writeBytes("--$boundary\r\n")
            writer.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n\r\n")
            writer.write(bytes)
            writer.writeBytes("\r\n")

            // End boundary
            writer.writeBytes("--$boundary--\r\n")
            writer.flush()
            writer.close()

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText().trim()
                // Parse direct URL from tmpfiles JSON response
                val urlIndex = response.indexOf("\"url\":\"")
                if (urlIndex != -1) {
                    val start = urlIndex + 7
                    val end = response.indexOf("\"", start)
                    if (end != -1) {
                        val rawUrl = response.substring(start, end).replace("\\/", "/")
                        // Convert viewer URL to direct download URL (replace tmpfiles.org/ with tmpfiles.org/dl/)
                        if (rawUrl.startsWith("https://tmpfiles.org/")) {
                            rawUrl.replace("https://tmpfiles.org/", "https://tmpfiles.org/dl/")
                        } else {
                            rawUrl
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
