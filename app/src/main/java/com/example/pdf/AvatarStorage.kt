package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AvatarStorage {
    private const val AVATAR_DIR = "avatars"
    private const val MAX_DIM_PX = 512

    fun avatarFile(context: Context, resumeId: String): File {
        val dir = File(context.applicationContext.filesDir, AVATAR_DIR)
        if (!dir.exists()) dir.mkdirs()
        val safeId = resumeId.replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifBlank { "draft" }
        return File(dir, "$safeId.jpg")
    }

    suspend fun savePickedPhoto(context: Context, resumeId: String, uri: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                val appContext = context.applicationContext
                val decoded = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@withContext null

                val scaled = scaleDownIfNeeded(decoded, MAX_DIM_PX)
                val dest = avatarFile(appContext, resumeId)
                FileOutputStream(dest).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (scaled != decoded) decoded.recycle()
                dest
            } catch (_: Exception) {
                null
            }
        }

    fun loadAvatarBitmap(context: Context, resumeId: String): Bitmap? {
        val file = avatarFile(context, resumeId)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteAvatar(context: Context, resumeId: String) {
        val file = avatarFile(context, resumeId)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun scaleDownIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
