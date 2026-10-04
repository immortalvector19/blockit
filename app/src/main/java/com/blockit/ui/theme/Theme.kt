package com.blockit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.blockit.domain.model.AccentTheme

data class BlockitColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryContainer: Color,
    val onPrimary: Color = Color.White
)

val LocalBlockitColors = staticCompositionLocalOf {
    BlockitColors(
        isDark = false,
        background = MachineLightBackground,
        surface = MachineLightSurface,
        surfaceVariant = MachineLightSurfaceVariant,
        border = MachineLightBorder,
        textPrimary = MachineLightText,
        textMuted = MachineLightTextMuted,
        primary = BlockitOrange,
        primaryDark = BlockitOrangeDark,
        primaryContainer = MachineLightSurfaceVariant
    )
}

object BlockitTheme {
    val colors: BlockitColors
        @Composable
        get() = LocalBlockitColors.current
}

@Composable
fun BlockItTheme(
    darkTheme: Boolean = false,
    accentTheme: AccentTheme = AccentTheme.ORANGE,
    content: @Composable () -> Unit
) {
    val primaryColor = Color(accentTheme.primaryColorHex)
    val blockitColors = if (darkTheme) {
        BlockitColors(
            isDark = true,
            background = MachineDarkBackground,
            surface = MachineDarkSurface,
            surfaceVariant = MachineDarkSurfaceVariant,
            border = MachineDarkBorder,
            textPrimary = MachineDarkText,
            textMuted = MachineDarkTextMuted,
            primary = primaryColor,
            primaryDark = primaryColor,
            primaryContainer = MachineDarkSurfaceVariant
        )
    } else {
        BlockitColors(
            isDark = false,
            background = MachineLightBackground,
            surface = MachineLightSurface,
            surfaceVariant = MachineLightSurfaceVariant,
            border = MachineLightBorder,
            textPrimary = MachineLightText,
            textMuted = MachineLightTextMuted,
            primary = primaryColor,
            primaryDark = primaryColor,
            primaryContainer = MachineLightSurfaceVariant
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = blockitColors.surfaceVariant,
            secondary = blockitColors.surface,
            background = blockitColors.background,
            surface = blockitColors.surface,
            surfaceVariant = blockitColors.surfaceVariant,
            onBackground = blockitColors.textPrimary,
            onSurface = blockitColors.textPrimary,
            error = MachineRed
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = blockitColors.surfaceVariant,
            secondary = CharcoalDark,
            background = blockitColors.background,
            surface = blockitColors.surface,
            surfaceVariant = blockitColors.surfaceVariant,
            onBackground = blockitColors.textPrimary,
            onSurface = blockitColors.textPrimary,
            error = MachineRed
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = blockitColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalBlockitColors provides blockitColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
