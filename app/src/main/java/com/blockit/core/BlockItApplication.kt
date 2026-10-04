package com.blockit.core

import android.app.Application
import android.content.Context
import com.blockit.core.di.AppContainer

class BlockItApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        fun from(context: Context): AppContainer {
            return (context.applicationContext as BlockItApplication).container
        }
    }
}
