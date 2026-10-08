package com.blockit.core.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, memory-efficient asynchronous icon and label cache.
 * Completely eliminates synchronous package manager IPC queries on the UI main thread.
 */
object AppIconCache {

    private val iconCache = LruCache<String, ImageBitmap>(200)
    private val labelCache = LruCache<String, String>(500)

    suspend fun getIcon(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext null
        iconCache.get(packageName)?.let { return@withContext it }

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = pm.getApplicationIcon(appInfo)
            val width = drawable.intrinsicWidth.coerceIn(48, 192)
            val height = drawable.intrinsicHeight.coerceIn(48, 192)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val imageBitmap = bitmap.asImageBitmap()
            iconCache.put(packageName, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLabel(context: Context, packageName: String): String = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext ""
        labelCache.get(packageName)?.let { return@withContext it }

        val label = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().ifBlank { packageName }
        } catch (_: Exception) {
            packageName
        }
        labelCache.put(packageName, label)
        label
    }

    fun getSyncLabelOrPackage(packageName: String): String {
        return labelCache.get(packageName) ?: packageName
    }

    fun clear() {
        iconCache.evictAll()
        labelCache.evictAll()
    }
}

/**
 * Composable helper that loads an application icon asynchronously off the UI main thread.
 */
@Composable
fun rememberAsyncAppIcon(packageName: String): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    return produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = AppIconCache.getIcon(context, packageName)
    }
}
