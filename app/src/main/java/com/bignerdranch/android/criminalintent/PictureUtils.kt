package com.bignerdranch.android.criminalintent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Loads a photo downsampled to roughly the target size, so a full-resolution camera
 * image doesn't blow through memory, and rotates it upright using its EXIF data.
 */
fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)

    val srcWidth = bounds.outWidth.toFloat()
    val srcHeight = bounds.outHeight.toFloat()
    if (srcWidth <= 0f || srcHeight <= 0f) return null

    val targetWidth = destWidth.coerceAtLeast(1)
    val targetHeight = destHeight.coerceAtLeast(1)

    val sampleSize = if (srcWidth <= targetWidth && srcHeight <= targetHeight) {
        1
    } else {
        minOf(srcWidth / targetWidth, srcHeight / targetHeight).roundToInt().coerceAtLeast(1)
    }

    val bitmap = BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return null

    val rotation = try {
        ExifInterface(path).rotationDegrees
    } catch (e: IOException) {
        0
    }
    if (rotation == 0) return bitmap

    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
