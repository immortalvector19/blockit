package com.blockit.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.blockit.domain.model.AccentTheme

data class FocusColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentContainer: Color,
    val statusActive: Color,
    val statusDestructive: Color,
    val onPrimary: Color = Color.White
) {
    val statusWarning: Color get() = if (isDark) WarningAmberDark else WarningAmber
    val warning: Color get() = statusWarning
    val divider: Color get() = border
    val onAccent: Color get() = onPrimary

    // Backward-compatible properties for existing screens
    val textMuted: Color get() = textSecondary
    val primary: Color get() = accent
    val primaryDark: Color get() = accent
    val primaryContainer: Color get() = accentContainer
}

// Backward-compatible typealias
typealias BlockitColors = FocusColors

val LocalFocusColors = staticCompositionLocalOf {
    FocusColors(
        isDark = false,
        background = FocusLightBackground,
        surface = FocusLightSurface,
        surfaceVariant = FocusLightSurfaceVariant,
        border = FocusLightBorder,
        textPrimary = FocusLightTextPrimary,
        textSecondary = FocusLightTextSecondary,
        textTertiary = FocusLightTextTertiary,
        accent = FocusLightAccent,
        accentContainer = FocusLightAccentContainer,
        statusActive = FocusLightStatusActive,
        statusDestructive = FocusLightStatusDestructive
    )
}

val LocalBlockitColors = LocalFocusColors

object FocusTheme {
    val colors: FocusColors
        @Composable
        get() = LocalFocusColors.current
    val typography: FocusTypography
        get() = FocusTypography
    val spacing: FocusSpacing
        get() = FocusSpacing
    val shapes: FocusShapes
        get() = FocusShapes
}

object BlockitTheme {
    val colors: FocusColors
        @Composable
        get() = LocalFocusColors.current
}

@Composable
fun FocusTheme(
    darkTheme: Boolean = false,
    accentTheme: AccentTheme = AccentTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val primaryAccent = Color(accentTheme.primaryColorHex)
    val accentContainerColor = if (darkTheme) {
        primaryAccent.copy(alpha = 0.2f)
    } else {
        primaryAccent.copy(alpha = 0.1f)
    }

    val focusColors = if (darkTheme) {
        FocusColors(
            isDark = true,
            background = FocusDarkBackground,
            surface = FocusDarkSurface,
            surfaceVariant = FocusDarkSurfaceVariant,
            border = FocusDarkBorder,
            textPrimary = FocusDarkTextPrimary,
            textSecondary = FocusDarkTextSecondary,
            textTertiary = FocusDarkTextTertiary,
            accent = primaryAccent,
            accentContainer = accentContainerColor,
            statusActive = FocusDarkStatusActive,
            statusDestructive = FocusDarkStatusDestructive
        )
    } else {
        FocusColors(
            isDark = false,
            background = FocusLightBackground,
            surface = FocusLightSurface,
            surfaceVariant = FocusLightSurfaceVariant,
            border = FocusLightBorder,
            textPrimary = FocusLightTextPrimary,
            textSecondary = FocusLightTextSecondary,
            textTertiary = FocusLightTextTertiary,
            accent = primaryAccent,
            accentContainer = accentContainerColor,
            statusActive = FocusLightStatusActive,
            statusDestructive = FocusLightStatusDestructive
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = focusColors.accent,
            onPrimary = Color.White,
            primaryContainer = focusColors.accentContainer,
            onPrimaryContainer = focusColors.textPrimary,
            secondary = focusColors.textSecondary,
            onSecondary = Color.White,
            background = focusColors.background,
            onBackground = focusColors.textPrimary,
            surface = focusColors.surface,
            onSurface = focusColors.textPrimary,
            surfaceVariant = focusColors.surfaceVariant,
            onSurfaceVariant = focusColors.textSecondary,
            outline = focusColors.border,
            error = focusColors.statusDestructive,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = focusColors.accent,
            onPrimary = Color.White,
            primaryContainer = focusColors.accentContainer,
            onPrimaryContainer = focusColors.textPrimary,
            secondary = focusColors.textSecondary,
            onSecondary = Color.White,
            background = focusColors.background,
            onBackground = focusColors.textPrimary,
            surface = focusColors.surface,
            onSurface = focusColors.textPrimary,
            surfaceVariant = focusColors.surfaceVariant,
            onSurfaceVariant = focusColors.textSecondary,
            outline = focusColors.border,
            error = focusColors.statusDestructive,
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = focusColors.background.toArgb()
            window.navigationBarColor = focusColors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalFocusColors provides focusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialFocusTypography,
            shapes = MaterialFocusShapes,
            content = content
        )
    }
}

/**
 * Backward-compatible wrapper for BlockItTheme
 */
@Composable
fun BlockItTheme(
    darkTheme: Boolean = false,
    accentTheme: AccentTheme = AccentTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    FocusTheme(
        darkTheme = darkTheme,
        accentTheme = accentTheme,
        content = content
    )
}
