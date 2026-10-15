package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.launch

object ImageUtils {
  private const val TAG = "ImageUtils"
  private const val FILE_PREFIX = "sensor_v2_"

  /**
   * Cleans legacy colliding cache files created by older versions.
   */
  fun cleanLegacyCache(context: Context) {
    try {
      context.cacheDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("sensor_img_")) {
          file.delete()
        }
      }
    } catch (_: Exception) {}
  }

  /**
   * Compresses an image picked from the gallery into a compact Base64 JPEG data URI string
   * (e.g. "data:image/jpeg;base64,...") suitable for Firestore cloud storage and multi-device sync.
   *
   * Automatically performs memory-safe downsampling, scales within [maxDimension],
   * respects EXIF orientation, and caches a local file with a full SHA-256 hash.
   */
  fun compressImageUriToBase64(
    context: Context,
    uri: Uri,
    maxDimension: Int = 800,
    quality: Int = 75
  ): String? {
    return try {
      val contentResolver = context.contentResolver

      // 1. Decode bounds only to prevent OOM on high-resolution camera images
      val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }

      val origWidth = boundsOptions.outWidth
      val origHeight = boundsOptions.outHeight
      if (origWidth <= 0 || origHeight <= 0) {
        Log.e(TAG, "Invalid image dimensions from URI: $uri")
        return null
      }

      // 2. Calculate sample size (power of 2)
      var inSampleSize = 1
      while ((origWidth / inSampleSize) > maxDimension * 2 || (origHeight / inSampleSize) > maxDimension * 2) {
        inSampleSize *= 2
      }

      // 3. Decode scaled down Bitmap
      val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.RGB_565 // Memory efficient for mobile photos
      }
      val sampledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: return null

      // 4. Check EXIF orientation
      val rotationDegrees = getExifRotation(context, uri)

      // 5. Calculate fine scaling to fit strictly within maxDimension
      val currentW = sampledBitmap.width
      val currentH = sampledBitmap.height
      val scale = if (currentW > maxDimension || currentH > maxDimension) {
        maxDimension.toFloat() / maxOf(currentW, currentH)
      } else {
        1.0f
      }

      val matrix = Matrix()
      if (scale < 1.0f) {
        matrix.postScale(scale, scale)
      }
      if (rotationDegrees != 0f) {
        matrix.postRotate(rotationDegrees)
      }

      val finalBitmap = if (!matrix.isIdentity) {
        val transformed = Bitmap.createBitmap(sampledBitmap, 0, 0, currentW, currentH, matrix, true)
        if (transformed != sampledBitmap) {
          sampledBitmap.recycle()
        }
        transformed
      } else {
        sampledBitmap
      }

      // 6. Compress to JPEG ByteArray
      val outputStream = ByteArrayOutputStream()
      finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      val jpegBytes = outputStream.toByteArray()
      finalBitmap.recycle()

      val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
      val dataUri = "data:image/jpeg;base64,$base64Data"

      // 7. Save a local cache file with full SHA-256 hash of the image content
      // Ensuring absolute uniqueness per photo and eliminating collisions
      try {
        val hash = sha256Bytes(jpegBytes)
        val cacheFile = File(context.cacheDir, "$FILE_PREFIX$hash.jpg")
        FileOutputStream(cacheFile).use { it.write(jpegBytes) }
      } catch (e: Exception) {
        Log.w(TAG, "Cache save skipped: ${e.message}")
      }

      dataUri
    } catch (e: Exception) {
      Log.e(TAG, "Failed to compress image URI to base64", e)
      null
    }
  }

  private val ioScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
  private val modelCache = java.util.concurrent.ConcurrentHashMap<String, Any>(128)

  fun getFastCacheKey(source: String): String {
    return if (source.startsWith("data:image")) {
      "b64_${source.length}_${source.take(64).hashCode().toUInt()}_${source.takeLast(64).hashCode().toUInt()}"
    } else {
      source
    }
  }

  fun preCacheBase64Image(context: Context, source: String): String {
    if (source.isBlank() || !source.startsWith("data:image")) return source
    val cacheKey = getFastCacheKey(source)
    val cacheFile = File(context.cacheDir, "$FILE_PREFIX$cacheKey.jpg")
    if (cacheFile.exists() && cacheFile.length() > 0) {
      modelCache[cacheKey] = cacheFile
      return cacheFile.absolutePath
    }
    // Asynchronously decode and save to file in background
    ioScope.launch {
      try {
        val base64Part = source.substringAfter("base64,")
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        FileOutputStream(cacheFile).use { it.write(bytes) }
        modelCache[cacheKey] = cacheFile
      } catch (_: Exception) {}
    }
    return cacheFile.absolutePath
  }

  /**
   * Returns a model usable by Coil (ImageRequest.Builder.data):
   * - File for cached base64 photos or local files
   * - ByteArray for freshly decoded Base64 URIs
   * - String URL for remote web links
   */
  fun getImageModel(context: Context, source: String?): Any? {
    if (source.isNullOrBlank()) return null
    val trimmed = source.trim()
    val cacheKey = getFastCacheKey(trimmed)

    modelCache[cacheKey]?.let { return it }

    val model: Any? = if (trimmed.startsWith("data:image")) {
      try {
        val cacheFile = File(context.cacheDir, "$FILE_PREFIX$cacheKey.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) {
          cacheFile
        } else {
          val base64Part = trimmed.substringAfter("base64,")
          val bytes = Base64.decode(base64Part, Base64.DEFAULT)
          // Write cache asynchronously without blocking UI thread
          ioScope.launch {
            try {
              FileOutputStream(cacheFile).use { it.write(bytes) }
            } catch (_: Exception) {}
          }
          bytes
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error decoding base64 image", e)
        null
      }
    } else if (trimmed.startsWith("/")) {
      val file = File(trimmed)
      if (file.exists()) file else null
    } else {
      trimmed
    }

    if (model != null) {
      modelCache[cacheKey] = model
    }
    return model
  }

  private fun getExifRotation(context: Context, uri: Uri): Float {
    return try {
      val tempFile = File.createTempFile("exif_probe", ".jpg", context.cacheDir)
      try {
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        val exif = ExifInterface(tempFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        when (orientation) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90f
          ExifInterface.ORIENTATION_ROTATE_180 -> 180f
          ExifInterface.ORIENTATION_ROTATE_270 -> 270f
          else -> 0f
        }
      } finally {
        tempFile.delete()
      }
    } catch (_: Exception) {
      0f
    }
  }

  private fun sha256Bytes(bytes: ByteArray): String {
    return try {
      val md = MessageDigest.getInstance("SHA-256")
      val digest = md.digest(bytes)
      digest.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
      bytes.size.toString() + "_" + bytes.hashCode().toString().replace("-", "n")
    }
  }

  private fun sha256String(input: String): String {
    return try {
      val md = MessageDigest.getInstance("SHA-256")
      val digest = md.digest(input.toByteArray(Charsets.ISO_8859_1))
      digest.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
      input.length.toString() + "_" + input.hashCode().toString().replace("-", "n")
    }
  }
}
