package com.blockit.core

import android.app.Application
import android.content.Context
import com.blockit.core.di.AppContainer
import kotlinx.coroutines.launch

class BlockItApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        if (com.blockit.BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apps = container.installedAppsRepository.getInstalledApps()
            apps.forEach { app ->
                com.blockit.core.cache.AppIconCache.getIcon(this@BlockItApplication, app.packageName)
            }
        }
    }

    companion object {
        fun from(context: Context): AppContainer {
            return (context.applicationContext as BlockItApplication).container
        }
    }
}
