package com.blockit.ui.screens.guide

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("How Blocking Works") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LayerCard(
                title = "Layer 1: App Interception & Compose Overlay",
                mechanism = "AccessibilityService (TYPE_WINDOW_STATE_CHANGED) + SYSTEM_ALERT_WINDOW",
                tradeoff = "Instant (<16ms) full-screen calm block screen directly over the distracting target app without killing process or restarting launcher. Uses UsageStatsManager polling as a secondary fallback if accessibility is ever paused.",
                icon = Icons.Default.Layers
            )

            LayerCard(
                title = "Layer 2: On-Device DNS VPN Filter",
                mechanism = "Local split-tunnel VpnService on UDP Port 53",
                tradeoff = "Intercepts domain lookups across all applications and browsers without sending packets off-device. Non-blocked DNS requests are forwarded to your physical router/carrier DNS via protected sockets. Synthetic NXDOMAIN responses prevent connection establishment entirely.",
                icon = Icons.Default.Dns
            )

            LayerCard(
                title = "Layer 3: Browser Address-Bar Inspection",
                mechanism = "Accessibility Node Extraction across 7 major browser engines",
                tradeoff = "Covers browsers utilizing Android Private DNS (DoT port 853 / DoH) which bypasses port 53 UDP. Inspects address bar View IDs on Chrome, Firefox, Brave, Samsung Internet, Edge, Opera, and DuckDuckGo to enforce domain and path rules.",
                icon = Icons.Default.Language
            )

            LayerCard(
                title = "Layer 4: AlarmManager & WorkManager Watchdogs",
                mechanism = "setExactAndAllowWhileIdle + Periodic Worker + BootReceiver",
                tradeoff = "Schedules evaluate precisely even in Android Doze mode. The device reboot receiver (RECEIVE_BOOT_COMPLETED) re-arms recurring alarms and active focus sessions immediately after reboot.",
                icon = Icons.Default.HourglassTop
            )

            LayerCard(
                title = "Layer 5: Anti-Bypass Strict Mode",
                mechanism = "Friction challenges, cooldown delays, and Device Admin",
                tradeoff = "Enforces cognitive friction (30s cooldowns or typing pledges) before disabling rules. Device Admin prevents uninstallation from settings/launcher during locked sessions.",
                icon = Icons.Default.Lock
            )
        }
    }
}

@Composable
private fun LayerCard(
    title: String,
    mechanism: String,
    tradeoff: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mechanism: $mechanism",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tradeoff,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
