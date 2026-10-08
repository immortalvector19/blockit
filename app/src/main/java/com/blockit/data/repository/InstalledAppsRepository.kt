package com.blockit.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.blockit.domain.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class InstalledAppsRepository(protected val context: Context? = null) {

    @Volatile
    private var cachedApps: List<InstalledAppInfo>? = null

    fun getCachedAppsSync(): List<InstalledAppInfo>? = cachedApps

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedApps != null) {
            return@withContext cachedApps!!
        }

        val ctx = context ?: return@withContext emptyList()
        val pm = ctx.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Query all activities that can be launched by user
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val selfPackage = ctx.packageName

        val apps = resolveInfos.mapNotNull { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val packageName = appInfo.packageName

            // Skip BlockIt itself to prevent self-locking
            if (packageName == selfPackage) return@mapNotNull null

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appName = resolveInfo.loadLabel(pm).toString().ifBlank { packageName }

            InstalledAppInfo(
                packageName = packageName,
                appName = appName,
                isSystemApp = isSystem
            )
        }.distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        cachedApps = apps
        apps
    }

    open fun resolveAppName(packageName: String): String {
        val ctx = context ?: return packageName
        return try {
            val pm = ctx.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().ifBlank { packageName }
        } catch (_: Exception) {
            packageName
        }
    }
}
