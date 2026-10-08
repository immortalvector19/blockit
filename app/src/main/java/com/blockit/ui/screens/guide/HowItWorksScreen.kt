package com.blockit.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun HowItWorksScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        topBar = {
            FocusTopBar(
                title = "How blocking works",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(FocusSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
        ) {
            // Privacy declaration card (the single place this is stated in the app)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.divider, FocusShapes.medium),
                shape = FocusShapes.medium,
                color = colors.surface
            ) {
                Row(
                    modifier = Modifier.padding(FocusSpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Privacy guaranteed",
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.md))
                    Column {
                        Text(
                            text = "100% on-device operation",
                            style = FocusTypography.bodyBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.xs))
                        Text(
                            text = "Zero network connections and no telemetry. All rule evaluation, statistics, and blocking logic run entirely within local system storage.",
                            style = FocusTypography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            FocusSectionHeader(title = "Enforcement layers")

            LayerCard(
                layerNumber = 1,
                title = "App overlay",
                mechanism = "Accessibility service window changes + system alert window",
                detail = "Displays an instant full-screen pause view directly over targeted apps without killing processes or resetting launcher state. Uses usage stats polling as a backup.",
                icon = Icons.Rounded.Layers
            )

            LayerCard(
                layerNumber = 2,
                title = "Local DNS filter",
                mechanism = "Local loopback VPN on port 53",
                detail = "Intercepts domain lookups across all browsers and apps without routing packets off-device. Non-blocked queries forward directly to your carrier or Wi-Fi DNS.",
                icon = Icons.Rounded.Dns
            )

            LayerCard(
                layerNumber = 3,
                title = "Browser address bar inspection",
                mechanism = "Accessibility node inspection across major browsers",
                detail = "Monitors address bar contents on Chrome, Firefox, Brave, Edge, Opera, Samsung Internet, and DuckDuckGo to enforce domain and path rules even over encrypted DNS.",
                icon = Icons.Rounded.Language
            )

            LayerCard(
                layerNumber = 4,
                title = "Exact alarms and watchdogs",
                mechanism = "Exact alarms + periodic workers + boot receiver",
                detail = "Schedules activate precisely at set times even in low-power Doze mode. The boot receiver restores active sessions and recurring routines after device restart.",
                icon = Icons.Rounded.HourglassTop
            )

            LayerCard(
                layerNumber = 5,
                title = "Anti-bypass controls",
                mechanism = "Friction countdowns, typing pledges, and device admin",
                detail = "Introduces deliberate cognitive pause before modifying active rules. Optional device admin prevents uninstallation while a focus session is active.",
                icon = Icons.Rounded.Lock
            )
        }
    }
}

@Composable
private fun LayerCard(
    layerNumber: Int,
    title: String,
    mechanism: String,
    detail: String,
    icon: ImageVector
) {
    val colors = FocusTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.divider, FocusShapes.medium),
        shape = FocusShapes.medium,
        color = colors.surface
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(FocusSpacing.sm))
                Text(
                    text = "Layer $layerNumber: $title",
                    style = FocusTypography.bodyBold,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(FocusSpacing.sm))

            Text(
                text = mechanism,
                style = FocusTypography.caption,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            Text(
                text = detail,
                style = FocusTypography.body,
                color = colors.textPrimary
            )
        }
    }
}
