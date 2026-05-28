package com.example.homeserve.data

import android.content.Context
import android.net.Uri

class StorageRepository {

    private val cloudinary = CloudinaryRepository()

    /**
     * Uploads a file to Cloudinary and returns the public https:// download URL.
     * If the URI is already an https:// URL, it is returned as-is (already uploaded).
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

        // Upload to Cloudinary
        return cloudinary.uploadImage(context, uri)
    }
}
