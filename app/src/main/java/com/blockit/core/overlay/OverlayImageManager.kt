package com.blockit.core.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Deep module for managing overlay background images and collages.
 * Persists selected images to private internal storage to guarantee longevity across
 * reboots, permission revocations, and system updates.
 */
object OverlayImageManager {

    private const val DIR_NAME = "overlay_images"
    private val memoryCache = LruCache<String, ImageBitmap>(16)

    fun getOverlayDirectory(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies and downsamples an image from [sourceUri] into private storage for [profileId].
     * @param slotIndex 0 for single image, 0..3 for 4-image collage.
     * @return Absolute file path of the persisted image.
     */
    suspend fun saveImage(
        context: Context,
        profileId: Long,
        slotIndex: Int,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val dir = getOverlayDirectory(context)
        val targetFile = File(dir, "profile_${profileId}_slot_${slotIndex}.jpg")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            val tempBytes = input.readBytes()
            if (tempBytes.isEmpty()) return@withContext ""

            // 1. Decode bounds to compute appropriate sample size
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, boundsOptions)

            val maxTargetDimension = 1920
            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxTargetDimension ||
                boundsOptions.outHeight / sampleSize > maxTargetDimension
            ) {
                sampleSize *= 2
            }

            // 2. Decode downsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory-friendly 16-bit color
            }
            val decodedBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)

            if (decodedBitmap != null) {
                FileOutputStream(targetFile).use { out ->
                    decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                decodedBitmap.recycle()
            }
        }

        val path = targetFile.absolutePath
        memoryCache.remove(path)
        path
    }

    /**
     * Asynchronously loads and caches an [ImageBitmap] from a local file path.
     */
    suspend fun loadBitmap(filePath: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (filePath.isBlank()) return@withContext null

        memoryCache.get(filePath)?.let { return@withContext it }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return@withContext null

        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            val imageBitmap = bitmap.asImageBitmap()
            memoryCache.put(filePath, imageBitmap)
            imageBitmap
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Deletes all overlay images associated with a given [profileId].
     */
    fun clearImagesForProfile(context: Context, profileId: Long) {
        val dir = getOverlayDirectory(context)
        dir.listFiles { _, name -> name.startsWith("profile_${profileId}_") }?.forEach {
            memoryCache.remove(it.absolutePath)
            it.delete()
        }
    }

    /**
     * Deletes a specific slot image for a given [profileId].
     */
    fun deleteSlotImage(context: Context, profileId: Long, slotIndex: Int) {
        val dir = getOverlayDirectory(context)
        val file = File(dir, "profile_${profileId}_slot_${slotIndex}.jpg")
        if (file.exists()) {
            memoryCache.remove(file.absolutePath)
            file.delete()
        }
    }
}
