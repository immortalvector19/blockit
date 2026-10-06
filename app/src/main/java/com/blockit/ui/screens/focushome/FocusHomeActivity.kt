package com.blockit.ui.screens.focushome

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.blockit.core.BlockItApplication
import com.blockit.domain.model.AccentTheme
import com.blockit.domain.model.SessionStrictness
import com.blockit.domain.model.ThemeMode
import com.blockit.ui.theme.BlockItTheme
import kotlinx.coroutines.launch

class FocusHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = BlockItApplication.from(this)

        // Trapped back press callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Focus Home is a lockdown kiosk launcher - back does not exit
            }
        })

        setContent {
            val themeMode by container.preferenceManager.themeMode.collectAsState(initial = ThemeMode.LIGHT)
            val accentTheme by container.preferenceManager.accentTheme.collectAsState(initial = AccentTheme.ORANGE)

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            SideEffect {
                val windowBg = if (isDark) "#191817" else "#ECEBE6"
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
                    val activeSession by container.focusSessionManager.observeActiveSession().collectAsState(initial = null)
                    val snapshot by container.ruleEvaluator.snapshotFlow.collectAsState()
                    val strictConfig by container.preferenceManager.strictModeConfig.collectAsState(initial = com.blockit.domain.model.StrictModeConfig())

                    val strictness = when {
                        strictConfig.isEnabled && strictConfig.requirePinToStopSession -> SessionStrictness.STRICT
                        else -> SessionStrictness.NORMAL
                    }

                    FocusHomeScreen(
                        session = activeSession,
                        profileName = snapshot.whitelistProfileName ?: "Whitelist",
                        permittedPackages = snapshot.whitelistPackages,
                        strictness = strictness,
                        onStopSession = {
                            val sessionId = activeSession?.id
                            if (sessionId != null) {
                                lifecycleScope.launch {
                                    container.focusSessionManager.endSession(sessionId)
                                    finish()
                                }
                            } else {
                                finish()
                            }
                        },
                        onSessionExpired = {
                            finish()
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
