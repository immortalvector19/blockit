package com.blockit.ui.screens.focushome

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.blockit.core.BlockItApplication
import com.blockit.domain.model.AccentTheme
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.SessionStrictness
import com.blockit.domain.model.ThemeMode
import com.blockit.ui.theme.BlockItTheme
import kotlinx.coroutines.launch

class FocusHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = BlockItApplication.from(this)

        // Fullscreen edge-to-edge and lockscreen coverage
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Trapped back press callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Focus Home is a lockdown kiosk launcher - back does not exit
            }
        })

        setContent {
            val themeMode by container.preferenceManager.themeMode.collectAsState(initial = ThemeMode.LIGHT)
            val accentTheme by container.preferenceManager.accentTheme.collectAsState(initial = AccentTheme.DEFAULT)

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            SideEffect {
                val windowBg = if (isDark) "#121316" else "#F8F9FA"
                window.decorView.setBackgroundColor(Color.parseColor(windowBg))
            }

            BlockItTheme(
                darkTheme = isDark,
                accentTheme = accentTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val activeSession by remember { container.focusSessionManager.observeActiveSession() }.collectAsState(
                        initial = container.focusSessionManager.getCachedActiveSession()
                    )
                    val snapshot by container.ruleEvaluator.snapshotFlow.collectAsState()
                    val strictConfig by container.preferenceManager.strictModeConfig.collectAsState(initial = com.blockit.domain.model.StrictModeConfig())

                    val strictness = when {
                        strictConfig.isEnabled && strictConfig.requirePinToStopSession -> SessionStrictness.STRICT
                        else -> SessionStrictness.NORMAL
                    }

                    val effectiveSession = activeSession ?: if (snapshot.hasActiveWhitelist && snapshot.activeScheduleEndTimestampMs != null) {
                        FocusSession(
                            id = -1L,
                            profileId = -1L,
                            startTimestamp = snapshot.activeScheduleStartTimestampMs ?: System.currentTimeMillis(),
                            endTimestamp = snapshot.activeScheduleEndTimestampMs ?: (System.currentTimeMillis() + 60_000L),
                            isActive = true
                        )
                    } else null

                    val isBreakActive by container.intentPassManager.isBreakActiveFlow.collectAsState()
                    val breakExpiresAtMs by container.intentPassManager.breakExpiresAtMsFlow.collectAsState()

                    FocusHomeScreen(
                        session = effectiveSession,
                        profileName = snapshot.whitelistProfileName ?: "Whitelist",
                        permittedPackages = snapshot.whitelistPackages,
                        strictness = strictness,
                        scheduleEndMs = snapshot.activeScheduleEndTimestampMs,
                        isBreakActive = isBreakActive,
                        breakExpiresAtMs = breakExpiresAtMs,
                        onGrantBreak = { durationMinutes ->
                            container.intentPassManager.grantBreak(durationMinutes)
                            val durationMs = durationMinutes * 60 * 1000L
                            val expiresAt = System.currentTimeMillis() + durationMs
                            if (container.overlayController.canDrawOverlays()) {
                                container.overlayController.showPauseCountdownPill(
                                    appName = "Break",
                                    expiresAtMs = expiresAt,
                                    onEndPauseEarly = {
                                        container.intentPassManager.endBreak()
                                        container.overlayController.hidePauseCountdownPill()
                                        val reopenIntent = Intent(this@FocusHomeActivity, FocusHomeActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        startActivity(reopenIntent)
                                    }
                                )
                            }
                            val homeLauncherIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeLauncherIntent)
                        },
                        onEndBreakEarly = {
                            container.intentPassManager.endBreak()
                            container.overlayController.hidePauseCountdownPill()
                        },
                        onSessionExpired = {
                            lifecycleScope.launch {
                                try {
                                    container.database.focusSessionDao().cleanupExpiredSessions(System.currentTimeMillis())
                                    container.ruleEvaluator.refreshRules()
                                } catch (_: Exception) {}
                                finish()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}
