package com.hellacious.spirecart.data.local.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

interface LocalImageStorage {
    suspend fun saveImageLocally(url: String): String
    fun getLocalImagePath(url: String): String?
}

class LocalImageStorageImpl(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : LocalImageStorage {

    private val imagesDir: File by lazy {
        File(context.filesDir, "product_images").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun hashUrl(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    override fun getLocalImagePath(url: String): String? {
        if (url.isBlank()) return null
        val filename = "${hashUrl(url)}.jpg"
        val file = File(imagesDir, filename)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    override suspend fun saveImageLocally(url: String): String = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext url

        val filename = "${hashUrl(url)}.jpg"
        val file = File(imagesDir, filename)

        if (file.exists() && file.length() > 0) {
            return@withContext file.absolutePath
        }

        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.exists() && file.length() > 0) {
                    return@withContext file.absolutePath
                }
            }
        } catch (e: Exception) {
            // In case of error, return original url fallback
        }
        return@withContext url
    }
}
