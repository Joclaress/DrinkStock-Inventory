package com.example.cokeinventory.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Resize bitmap to max 300px dimension and 50% JPEG quality to save KB storage size
        val maxDim = 300
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            if (aspect > 1) {
                Bitmap.createScaledBitmap(bitmap, maxDim, (maxDim / aspect).toInt(), true)
            } else {
                Bitmap.createScaledBitmap(bitmap, (maxDim * aspect).toInt(), maxDim, true)
            }
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
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
