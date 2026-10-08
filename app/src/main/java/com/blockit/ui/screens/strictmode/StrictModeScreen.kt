package com.blockit.ui.screens.strictmode

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.domain.model.ChallengeType
import com.blockit.domain.model.StrictModeConfig
import com.blockit.service.admin.BlockItDeviceAdminReceiver
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusSwitchRow
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import kotlinx.coroutines.launch

@Composable
fun StrictModeScreen(
    preferenceManager: PreferenceManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = FocusTheme.colors
    val config by preferenceManager.strictModeConfig.collectAsState(initial = StrictModeConfig())

    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, BlockItDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    Scaffold(
        modifier = modifier,
        topBar = {
            FocusTopBar(
                title = "Strict mode",
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
            // Master Switch
            FocusSwitchRow(
                title = "Enable strict mode",
                subtitle = "Adds intentional friction before turning off active focus rules or ending sessions early.",
                checked = config.isEnabled,
                onCheckedChange = { isEnabled ->
                    scope.launch {
                        preferenceManager.updateStrictMode(config.copy(isEnabled = isEnabled))
                    }
                }
            )

            // Device Admin Protection
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Uninstall protection")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FocusShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(FocusSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = if (isAdminActive) colors.statusActive else colors.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(FocusSpacing.xs))
                                Text(
                                    text = "Device admin",
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                            }

                            if (isAdminActive) {
                                Surface(
                                    shape = FocusShapes.small,
                                    color = colors.statusActive.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Active",
                                        color = colors.statusActive,
                                        style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(FocusSpacing.xs))
                        Text(
                            text = "Prevents casual uninstallation of FocusShield from settings or home screen while focus sessions are locked.",
                            style = FocusTypography.caption,
                            color = colors.textMuted
                        )

                        if (!isAdminActive) {
                            Spacer(modifier = Modifier.height(FocusSpacing.md))
                            FocusButton(
                                text = "Activate device admin",
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
                            )
                        }
                    }
                }
            }

            // Challenge Selection
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                FocusSectionHeader(title = "Friction challenge")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FocusShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(FocusSpacing.md)) {
                        Text(
                            text = "Select challenge type",
                            style = FocusTypography.label,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.xs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                        ) {
                            ChallengeChip(
                                label = "PIN lock",
                                isSelected = config.challengeType == ChallengeType.PIN_PASSWORD,
                                onClick = {
                                    scope.launch {
                                        preferenceManager.updateStrictMode(config.copy(challengeType = ChallengeType.PIN_PASSWORD))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChallengeChip(
                                label = "Cooldown",
                                isSelected = config.challengeType == ChallengeType.COOLDOWN,
                                onClick = {
                                    scope.launch {
                                        preferenceManager.updateStrictMode(config.copy(challengeType = ChallengeType.COOLDOWN))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChallengeChip(
                                label = "Passage",
                                isSelected = config.challengeType == ChallengeType.TYPED_PASSAGE,
                                onClick = {
                                    scope.launch {
                                        preferenceManager.updateStrictMode(config.copy(challengeType = ChallengeType.TYPED_PASSAGE))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(FocusSpacing.md))

                        if (config.challengeType == ChallengeType.PIN_PASSWORD) {
                            Text(
                                text = "Security PIN",
                                style = FocusTypography.label,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(FocusSpacing.xs))

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
                                label = { Text("4-digit PIN") },
                                placeholder = { Text("e.g. 1234") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = FocusShapes.small,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(FocusSpacing.md))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Require PIN to stop session",
                                        style = FocusTypography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Prevents stopping active sessions without entering the PIN.",
                                        style = FocusTypography.caption,
                                        color = colors.textMuted
                                    )
                                }
                                Switch(
                                    checked = config.requirePinToStopSession,
                                    onCheckedChange = { req ->
                                        scope.launch {
                                            preferenceManager.updateStrictMode(config.copy(requirePinToStopSession = req))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.primary
                                    )
                                )
                            }
                        } else if (config.challengeType == ChallengeType.COOLDOWN) {
                            Text(
                                text = "Cooldown duration",
                                style = FocusTypography.label,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(FocusSpacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                            ) {
                                listOf(30 to "30s", 60 to "60s", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
                                    ChallengeChip(
                                        label = label,
                                        isSelected = config.cooldownSeconds == sec,
                                        onClick = {
                                            scope.launch {
                                                preferenceManager.updateStrictMode(config.copy(cooldownSeconds = sec))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            var passageInput by remember(config.targetPassage) { mutableStateOf(config.targetPassage) }
                            Text(
                                text = "Reflection passage",
                                style = FocusTypography.label,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(FocusSpacing.xs))
                            OutlinedTextField(
                                value = passageInput,
                                onValueChange = {
                                    passageInput = it
                                    scope.launch {
                                        preferenceManager.updateStrictMode(config.copy(targetPassage = it))
                                    }
                                },
                                label = { Text("Passage text") },
                                singleLine = false,
                                maxLines = 4,
                                shape = FocusShapes.small,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    Surface(
        onClick = onClick,
        shape = FocusShapes.small,
        color = if (isSelected) colors.primary else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.border),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = FocusSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = FocusTypography.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (isSelected) Color.White else colors.textPrimary
            )
        }
    }
}
