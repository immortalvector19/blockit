package com.blockit.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.blockit.ui.overlay.BlockScreenContent
import com.blockit.ui.overlay.BlockOverlayActivity

class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    @Volatile
    private var isShowing = false

    fun showBlockOverlay(
        targetIdentifier: String,
        profileName: String,
        reason: String,
        onHomeClicked: () -> Unit
    ) {
        mainHandler.post {
            if (isShowing) return@post

            // If SYSTEM_ALERT_WINDOW permission is granted, use instant full-screen Compose overlay
            if (Settings.canDrawOverlays(context)) {
                try {
                    val owner = OverlayLifecycleOwner()
                    lifecycleOwner = owner

                    val composeView = ComposeView(context).apply {
                        setViewTreeLifecycleOwner(owner)
                        setViewTreeViewModelStoreOwner(owner)
                        setViewTreeSavedStateRegistryOwner(owner)
                        setContent {
                            BlockScreenContent(
                                targetIdentifier = targetIdentifier,
                                profileName = profileName,
                                reason = reason,
                                onHomeClicked = {
                                    hideBlockOverlay()
                                    onHomeClicked()
                                }
                            )
                        }
                    }

                    val layoutParams = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        } else {
                            @Suppress("DEPRECATION")
                            WindowManager.LayoutParams.TYPE_PHONE
                        },
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    windowManager.addView(composeView, layoutParams)
                    overlayView = composeView
                    isShowing = true
                    return@post
                } catch (_: Exception) {
                    // Fall back to Activity launch if WindowManager addView fails
                }
            }

            // Fallback: Launch standalone BlockOverlayActivity
            BlockOverlayActivity.start(context, targetIdentifier, profileName, reason)
        }
    }

    fun hideBlockOverlay() {
        mainHandler.post {
            overlayView?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (_: Exception) {}
                overlayView = null
            }
            lifecycleOwner?.destroy()
            lifecycleOwner = null
            isShowing = false
        }
    }

    fun isOverlayShowing(): Boolean = isShowing
}
