package com.blockit.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings & Tools") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsNavigationCard(
                title = "Engine Health & Diagnostics",
                subtitle = "Live accessibility status, event timestamp, overlay test, and battery exemption",
                icon = Icons.Default.MonitorHeart,
                onClick = onNavigateToHealth
            )

            SettingsNavigationCard(
                title = "Always Allowed Apps",
                subtitle = "Phone dialer and emergency apps exempt from all focus blocks",
                icon = Icons.Default.Phone,
                onClick = onNavigateToAlwaysAllowed
            )

            SettingsNavigationCard(
                title = "Appearance & Themes",
                subtitle = "Dark mode toggle, cream paper aesthetic, and mechanical accent palettes",
                icon = Icons.Default.Palette,
                onClick = onNavigateToThemeSettings
            )

            SettingsNavigationCard(
                title = "Strict Mode & Anti-Bypass",
                subtitle = "Friction challenges, cooldown delays, and anti-uninstall Device Admin locks",
                icon = Icons.Default.Lock,
                onClick = onNavigateToStrictMode
            )

            SettingsNavigationCard(
                title = "Permissions & Setup Guide",
                subtitle = "Service status checklist and OEM background optimization bypass guides",
                icon = Icons.Default.Build,
                onClick = onNavigateToSetupGuide
            )

            SettingsNavigationCard(
                title = "Interactive Onboarding Tour",
                subtitle = "Replay the initial setup guide and verify required system permissions",
                icon = Icons.Default.Info,
                onClick = onNavigateToOnboarding
            )

            SettingsNavigationCard(
                title = "Backup & Restore",
                subtitle = "Export and restore profiles, schedules, and rules to local JSON files",
                icon = Icons.Default.Storage,
                onClick = onNavigateToBackupRestore
            )

            SettingsNavigationCard(
                title = "How Blocking Works",
                subtitle = "Technical architecture, DNS interception, and layer tradeoff documentation",
                icon = Icons.Default.Info,
                onClick = onNavigateToHowItWorks
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FocusShield v1.0.0 (Personal Edition)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "100% On-Device | Zero Internet | No Telemetry | No Ads",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
