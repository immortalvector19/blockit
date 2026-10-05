package com.blockit.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

data class OverlayData(
    val targetIdentifier: String,
    val profileName: String,
    val reason: String,
    val isWhitelistBlock: Boolean = false,
    val allowedApps: List<String> = emptyList(),
    val isHardLock: Boolean = false,
    val canRequestPass: Boolean = false,
    val remainingPasses: Int = 0,
    val onHomeClicked: () -> Unit,
    val onOpenAllowedApp: ((String) -> Unit)? = null,
    val onGrantPass: (() -> Unit)? = null
)

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

    private var currentData by mutableStateOf<OverlayData?>(null)

    @Volatile
    private var isShowing = false

    fun showBlockOverlay(
        targetIdentifier: String,
        profileName: String,
        reason: String,
        isWhitelistBlock: Boolean = false,
        allowedApps: List<String> = emptyList(),
        isHardLock: Boolean = false,
        canRequestPass: Boolean = false,
        remainingPasses: Int = 0,
        onHomeClicked: () -> Unit,
        onOpenAllowedApp: ((String) -> Unit)? = null,
        onGrantPass: (() -> Unit)? = null
    ) {
        val data = OverlayData(
            targetIdentifier = targetIdentifier,
            profileName = profileName,
            reason = reason,
            isWhitelistBlock = isWhitelistBlock,
            allowedApps = allowedApps,
            isHardLock = isHardLock,
            canRequestPass = canRequestPass,
            remainingPasses = remainingPasses,
            onHomeClicked = onHomeClicked,
            onOpenAllowedApp = onOpenAllowedApp,
            onGrantPass = onGrantPass
        )

        mainHandler.post {
            currentData = data

            if (isShowing) {
                // Overlay already attached to windowManager; Compose will reactively update
                return@post
            }

            // If SYSTEM_ALERT_WINDOW permission is granted, use instant full-screen Compose overlay
            if (Settings.canDrawOverlays(context)) {
                try {
                    val themedContext = android.view.ContextThemeWrapper(context, com.blockit.R.style.Theme_BlockIt)
                    val owner = OverlayLifecycleOwner()
                    lifecycleOwner = owner

                    val container = object : android.widget.FrameLayout(themedContext) {
                        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                                currentData?.onHomeClicked?.invoke()
                                return true
                            }
                            return super.dispatchKeyEvent(event)
                        }
                    }

                    // Attach lifecycle owners to container (the window root view)
                    container.setViewTreeLifecycleOwner(owner)
                    container.setViewTreeViewModelStoreOwner(owner)
                    container.setViewTreeSavedStateRegistryOwner(owner)

                    val composeView = ComposeView(themedContext).apply {
                        setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                        setViewTreeLifecycleOwner(owner)
                        setViewTreeViewModelStoreOwner(owner)
                        setViewTreeSavedStateRegistryOwner(owner)
                        setContent {
                            val active = currentData
                            if (active != null) {
                                BlockScreenContent(
                                    targetIdentifier = active.targetIdentifier,
                                    profileName = active.profileName,
                                    reason = active.reason,
                                    isWhitelistBlock = active.isWhitelistBlock,
                                    allowedApps = active.allowedApps,
                                    isHardLock = active.isHardLock,
                                    canRequestPass = active.canRequestPass,
                                    remainingPasses = active.remainingPasses,
                                    onHomeClicked = {
                                        active.onHomeClicked()
                                    },
                                    onOpenAllowedApp = { pkg ->
                                        active.onOpenAllowedApp?.invoke(pkg)
                                    },
                                    onGrantPass = {
                                        active.onGrantPass?.invoke()
                                    }
                                )
                            }
                        }
                    }

                    container.addView(
                        composeView,
                        android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )

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
                                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    windowManager.addView(container, layoutParams)
                    overlayView = container
                    isShowing = true
                    return@post
                } catch (e: Exception) {
                    android.util.Log.e("OverlayController", "Failed to add window overlay view", e)
                    // Fall back to Activity launch if WindowManager addView fails
                }
            }

            // Fallback: Launch standalone BlockOverlayActivity
            BlockOverlayActivity.start(context, targetIdentifier, profileName, reason)
        }
    }

    fun hideBlockOverlay() {
        mainHandler.post {
            currentData = null
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
