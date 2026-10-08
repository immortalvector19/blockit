package com.blockit.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun SettingsHubScreen(
    onNavigateToHealth: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToStrictMode: () -> Unit,
    onNavigateToAlwaysAllowed: () -> Unit = {},
    onNavigateToSetupGuide: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToHowItWorks: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors

    Scaffold(
        modifier = modifier,
        topBar = {
            FocusTopBar(
                title = "Settings",
                onBackClick = onBackClick
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
        ) {
            // General Settings
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "General")
                SettingsRow(
                    title = "Always allowed apps",
                    subtitle = "Dialer and emergency apps exempt from blocking",
                    icon = Icons.Default.Phone,
                    onClick = onNavigateToAlwaysAllowed
                )
                SettingsRow(
                    title = "Appearance",
                    subtitle = "Theme mode and accent color",
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToThemeSettings
                )
                SettingsRow(
                    title = "Strict mode",
                    subtitle = "Friction challenges and bypass prevention",
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToStrictMode
                )
            }

            // System & Diagnostics
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Diagnostics")
                SettingsRow(
                    title = "System health",
                    subtitle = "Accessibility service, overlay, and battery status",
                    icon = Icons.Default.MonitorHeart,
                    onClick = onNavigateToHealth
                )
                SettingsRow(
                    title = "Setup guide",
                    subtitle = "Permission instructions for OEM battery savers",
                    icon = Icons.Default.Build,
                    onClick = onNavigateToSetupGuide
                )
                SettingsRow(
                    title = "Setup checklist",
                    subtitle = "Reopen the required permissions checklist",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToOnboarding
                )
            }

            // Data & Documentation
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Data and privacy")
                SettingsRow(
                    title = "Backup and restore",
                    subtitle = "Export or import profiles and schedules",
                    icon = Icons.Default.Storage,
                    onClick = onNavigateToBackupRestore
                )
                SettingsRow(
                    title = "How blocking works",
                    subtitle = "On-device architecture and privacy verification",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToHowItWorks
                )
            }

            Spacer(modifier = Modifier.height(FocusSpacing.md))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FocusSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FocusShield v1.0.0",
                    style = FocusTypography.caption,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = FocusTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = FocusShapes.medium,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(FocusSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FocusTypography.caption,
                    color = colors.textMuted
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
