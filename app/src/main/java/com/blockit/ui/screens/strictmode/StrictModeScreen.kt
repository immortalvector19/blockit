package com.blockit.ui.screens.strictmode

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.domain.model.ChallengeType
import com.blockit.domain.model.StrictModeConfig
import com.blockit.service.admin.BlockItDeviceAdminReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrictModeScreen(
    preferenceManager: PreferenceManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by preferenceManager.strictModeConfig.collectAsState(initial = StrictModeConfig())

    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, BlockItDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Strict Mode & Anti-Bypass") },
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
            // Master Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (config.isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Strict Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Adds intentional friction before weakening or turning off active blocking rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { isEnabled ->
                            scope.launch {
                                preferenceManager.updateStrictMode(config.copy(isEnabled = isEnabled))
                            }
                        }
                    )
                }
            }

            // Device Admin Protection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (isAdminActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Device Admin (Uninstall Protection)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Prevents casual uninstallation of FocusShield while you have active focus rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAdminActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active: Uninstall Protected", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF10B981))
                        }
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "Required to protect active focus sessions from uninstallation."
                                    )
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Activate Device Admin")
                        }
                    }
                }
            }

            // Challenge Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Friction Challenge Type", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.challengeType == ChallengeType.PIN_PASSWORD,
                            onClick = {
                                scope.launch {
                                    preferenceManager.updateStrictMode(config.copy(challengeType = ChallengeType.PIN_PASSWORD))
                                }
                            },
                            label = { Text("PIN Lock") }
                        )
                        FilterChip(
                            selected = config.challengeType == ChallengeType.COOLDOWN,
                            onClick = {
                                scope.launch {
                                    preferenceManager.updateStrictMode(config.copy(challengeType = ChallengeType.COOLDOWN))
                                }
                            },
                            label = { Text("Cooldown Delay") }
                        )
                        FilterChip(
                            selected = config.challengeType == ChallengeType.TYPED_PASSAGE,
                            onClick = {
                                scope.launch {
                                    preferenceManager.updateStrictMode(config.copy(challengeType = ChallengeType.TYPED_PASSAGE))
                                }
                            },
                            label = { Text("Typed Passage") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (config.challengeType == ChallengeType.PIN_PASSWORD) {
                        Text("Security PIN", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))

                        var pinInput by remember(config.unlockPin) { mutableStateOf(config.unlockPin) }
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(4)
                                pinInput = digits
                                scope.launch {
                                    preferenceManager.updateStrictMode(config.copy(unlockPin = digits))
                                }
                            },
                            label = { Text("4-Digit Security PIN") },
                            placeholder = { Text("e.g. 1234") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Require PIN to Stop Session", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Prevents stopping or cancelling active focus sessions without the PIN.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = config.requirePinToStopSession,
                                onCheckedChange = { req ->
                                    scope.launch {
                                        preferenceManager.updateStrictMode(config.copy(requirePinToStopSession = req))
                                    }
                                }
                            )
                        }
                    } else if (config.challengeType == ChallengeType.COOLDOWN) {
                        Text("Cooldown Duration", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30 to "30s", 60 to "60s", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
                                FilterChip(
                                    selected = config.cooldownSeconds == sec,
                                    onClick = {
                                        scope.launch {
                                            preferenceManager.updateStrictMode(config.copy(cooldownSeconds = sec))
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    } else {
                        var passageInput by remember(config.targetPassage) { mutableStateOf(config.targetPassage) }
                        OutlinedTextField(
                            value = passageInput,
                            onValueChange = {
                                passageInput = it
                                scope.launch {
                                    preferenceManager.updateStrictMode(config.copy(targetPassage = it))
                                }
                            },
                            label = { Text("Reflection Passage") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Settings App Guard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Protect Android Settings", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Blocks attempts to force stop or disable FocusShield in device Settings during active sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = config.protectSettings,
                        onCheckedChange = { protect ->
                            scope.launch {
                                preferenceManager.updateStrictMode(config.copy(protectSettings = protect))
                            }
                        }
                    )
                }
            }

            // Max Pauses Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Max Pauses per Session", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Limit how many times you can pause a blocked app during focus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "0", 1 to "1", 3 to "3", 5 to "5").forEach { (count, label) ->
                            FilterChip(
                                selected = config.maxPausesPerSession == count,
                                onClick = {
                                    scope.launch {
                                        preferenceManager.updateStrictMode(config.copy(maxPausesPerSession = count))
                                    }
                                },
                                label = { Text(if (count == 0) "No pause" else "$label pauses") }
                            )
                        }
                    }
                }
            }
        }
    }
}
