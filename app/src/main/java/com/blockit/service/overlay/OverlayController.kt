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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.overlay.BlockOverlayActivity
import com.blockit.ui.overlay.BlockScreenContent
import kotlinx.coroutines.delay

data class OverlayData(
    val targetIdentifier: String,
    val profileName: String,
    val reason: String,
    val strictness: SessionStrictness = SessionStrictness.NORMAL,
    val canRequestPause: Boolean = true,
    val remainingPauses: Int = 3,
    val maxPauses: Int = 3,
    val onHomeClicked: () -> Unit,
    val onGrantPause: ((durationMinutes: Int) -> Unit)? = null
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

    private var currentData: OverlayData? = null

    private var pillView: View? = null
    private var pillLifecycleOwner: OverlayLifecycleOwner? = null

    @Volatile
    private var isShowing = false

    fun showBlockOverlay(
        targetIdentifier: String,
        profileName: String,
        reason: String,
        strictness: SessionStrictness = SessionStrictness.NORMAL,
        canRequestPause: Boolean = true,
        remainingPauses: Int = 3,
        maxPauses: Int = 3,
        onHomeClicked: () -> Unit,
        onGrantPause: ((durationMinutes: Int) -> Unit)? = null
    ) {
        val data = OverlayData(
            targetIdentifier = targetIdentifier,
            profileName = profileName,
            reason = reason,
            strictness = strictness,
            canRequestPause = canRequestPause,
            remainingPauses = remainingPauses,
            maxPauses = maxPauses,
            onHomeClicked = onHomeClicked,
            onGrantPause = onGrantPause
        )

        mainHandler.post {
            currentData = data

            if (isShowing) {
                return@post
            }

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

                    container.setViewTreeLifecycleOwner(owner)
                    container.setViewTreeViewModelStoreOwner(owner)
                    container.setViewTreeSavedStateRegistryOwner(owner)

                    val composeView = ComposeView(themedContext).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
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
                                    strictness = active.strictness,
                                    canRequestPause = active.canRequestPause,
                                    remainingPauses = active.remainingPauses,
                                    maxPauses = active.maxPauses,
                                    onHomeClicked = {
                                        active.onHomeClicked()
                                    },
                                    onGrantPause = { minutes ->
                                        active.onGrantPause?.invoke(minutes)
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
                }
            }

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

    fun showPauseCountdownPill(
        appName: String,
        expiresAtMs: Long,
        onEndPauseEarly: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return

        mainHandler.post {
            hidePauseCountdownPill()
            try {
                val themedContext = android.view.ContextThemeWrapper(context, com.blockit.R.style.Theme_BlockIt)
                val owner = OverlayLifecycleOwner()
                pillLifecycleOwner = owner

                val composeView = ComposeView(themedContext).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setContent {
                        PauseCountdownPill(
                            appName = appName,
                            expiresAtMs = expiresAtMs,
                            onEndEarly = onEndPauseEarly
                        )
                    }
                }

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 60
                }

                windowManager.addView(composeView, layoutParams)
                pillView = composeView
            } catch (e: Exception) {
                android.util.Log.e("OverlayController", "Failed to add pause countdown pill", e)
            }
        }
    }

    fun hidePauseCountdownPill() {
        mainHandler.post {
            pillView?.let {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {}
                pillView = null
            }
            pillLifecycleOwner?.destroy()
            pillLifecycleOwner = null
        }
    }

    fun isOverlayShowing(): Boolean = isShowing

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)
}

@Composable
fun PauseCountdownPill(
    appName: String,
    expiresAtMs: Long,
    onEndEarly: () -> Unit
) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val remainingSec = ((expiresAtMs - currentTimeMs) / 1000).coerceAtLeast(0)
    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val timerStr = String.format("%02d:%02d", mins, secs)

    LaunchedEffect(remainingSec) {
        if (remainingSec <= 0L) {
            onEndEarly()
        }
    }

    Surface(
        color = Color(0xF00F172A),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$appName: $timerStr",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = "End",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF331B24))
                    .clickable { onEndEarly() }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
