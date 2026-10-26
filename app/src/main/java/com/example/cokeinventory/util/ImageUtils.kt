package com.example.cokeinventory.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Ultra-Compact Image Compression for Firestore
 *
 * Optimizes photos down to ~5 KB - 12 KB to minimize Firestore storage usage,
 * accelerate network load speeds, and prevent document size limits.
 */
object ImageUtils {
    fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 180, quality: Int = 35): String {
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Calculate aspect ratio and scale down image to max 180px thumbnail
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            if (aspect > 1f) {
                Bitmap.createScaledBitmap(bitmap, maxDimension, (maxDimension / aspect).toInt().coerceAtLeast(1), true)
            } else {
                Bitmap.createScaledBitmap(bitmap, (maxDimension * aspect).toInt().coerceAtLeast(1), maxDimension, true)
            }
        } else {
            bitmap
        }

        // Compress image using highly optimized JPEG compression (35% quality)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        
        // Clean up scaled bitmap reference if a copy was created
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun base64ToImageBitmap(base64Str: String?): ImageBitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
