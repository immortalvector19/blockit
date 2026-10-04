package com.blockit.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.blockit.core.BlockItApplication
import com.blockit.ui.navigation.BlockItNavHost
import com.blockit.ui.theme.BlockItTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.blockit.domain.model.AccentTheme
import com.blockit.domain.model.ThemeMode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = BlockItApplication.from(this)

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
                    val navController = rememberNavController()
                    BlockItNavHost(
                        navController = navController,
                        container = container,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}
