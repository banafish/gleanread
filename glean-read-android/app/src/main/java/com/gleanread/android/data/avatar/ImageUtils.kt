package com.gleanread.android.data.avatar
 
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
 
data class CompressedImage(
    val bytes: ByteArray,
    val mimeType: String,
    val extension: String
)
 
object ImageUtils {
    private const val MAX_SIZE_KB = 500
    private const val MAX_DIMENSION = 1024
 
    fun compressImage(context: Context, uri: Uri): CompressedImage? {
        return try {
            val originalMimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
 
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
 
            if (bitmap == null) return null
 
            // Handle Exif orientation
            val exifInputStream = context.contentResolver.openInputStream(uri)
            if (exifInputStream != null) {
                val exif = ExifInterface(exifInputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                exifInputStream.close()
 
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
 
            // Scale down if too large
            if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
                val ratio = minOf(MAX_DIMENSION.toFloat() / bitmap.width, MAX_DIMENSION.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }
 
            // Select compression format
            val (format, targetMimeType, targetExtension) = when {
                originalMimeType == "image/png" -> Triple(Bitmap.CompressFormat.PNG, "image/png", "png")
                android.os.Build.VERSION.SDK_INT >= 30 && originalMimeType == "image/webp" -> 
                    Triple(Bitmap.CompressFormat.WEBP_LOSSLESS, "image/webp", "webp")
                else -> Triple(Bitmap.CompressFormat.JPEG, "image/jpeg", "jpg")
            }
 
            // Compress
            var quality = 100
            var outputStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outputStream)
 
            // For formats that support quality adjustment (JPEG/WEBP)
            if (format != Bitmap.CompressFormat.PNG && format != Bitmap.CompressFormat.WEBP_LOSSLESS) {
                while (outputStream.toByteArray().size / 1024 > MAX_SIZE_KB && quality > 10) {
                    quality -= 10
                    outputStream.reset()
                    bitmap.compress(format, quality, outputStream)
                }
            }
 
            CompressedImage(
                bytes = outputStream.toByteArray(),
                mimeType = targetMimeType,
                extension = targetExtension
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
