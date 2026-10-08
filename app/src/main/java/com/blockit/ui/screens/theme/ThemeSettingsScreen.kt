package com.blockit.ui.screens.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.domain.model.AccentTheme
import com.blockit.domain.model.ThemeMode
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val colors = FocusTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            FocusTopBar(
                title = "Appearance",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
        ) {
            // Theme Mode
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Theme mode")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                ) {
                    ThemeModeCard(
                        title = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeModeCard(
                        title = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeModeCard(
                        title = "System",
                        icon = Icons.Default.SettingsBrightness,
                        isSelected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Accent Color
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Accent color")
                Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                    AccentTheme.entries.forEach { theme ->
                        val isSelected = accentTheme == theme
                        val themeColor = Color(theme.primaryColorHex)

                        Surface(
                            onClick = { viewModel.setAccentTheme(theme) },
                            shape = FocusShapes.medium,
                            color = colors.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) colors.primary else colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FocusSpacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(themeColor)
                                    )
                                    Spacer(modifier = Modifier.width(FocusSpacing.md))
                                    Text(
                                        text = theme.displayName,
                                        style = FocusTypography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = colors.textPrimary
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Preview Card
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Preview")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FocusShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FocusSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
                    ) {
                        Text(
                            text = "Work focus",
                            style = FocusTypography.title,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "25 minutes remaining · Blocklist mode active",
                            style = FocusTypography.bodySmall,
                            color = colors.textMuted
                        )
                        FocusButton(
                            text = "Pause focus",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = FocusShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.border
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FocusSpacing.md, horizontal = FocusSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) colors.primary else colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(FocusSpacing.xs))
            Text(
                text = title,
                style = FocusTypography.caption.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isSelected) colors.primary else colors.textPrimary
            )
        }
    }
}
