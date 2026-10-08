package com.blockit.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusMonolithMark
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingChecklistSheet(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    isBatteryIgnored: Boolean,
    isPinConfigured: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit,
    onEnableBattery: () -> Unit,
    onSetPin: (String) -> Unit,
    onComplete: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    canDismiss: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = FocusTheme.colors
    var showPinDialog by remember { mutableStateOf(false) }

    val requiredCount = 3
    var grantedCount = 0
    if (isAccessibilityEnabled) grantedCount++
    if (isOverlayEnabled) grantedCount++
    if (isBatteryIgnored) grantedCount++
    val allRequiredGranted = grantedCount == requiredCount

    ModalBottomSheet(
        onDismissRequest = {
            if (canDismiss || allRequiredGranted) {
                onDismissRequest()
            }
        },
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = FocusShapes.large,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Header: Title + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FocusMonolithMark(
                    size = 22.dp,
                    tint = colors.primary
                )
                Column {
                    Text(
                        text = "Setup checklist",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (allRequiredGranted) "All required permissions granted"
                        else "$grantedCount of $requiredCount required items complete",
                        style = FocusTypography.caption,
                        color = if (allRequiredGranted) colors.statusActive else colors.textSecondary
                    )
                }
            }

            // Compact Checklist Rows with 12dp spacing
            ChecklistRow(
                title = "Accessibility service",
                description = "Detects foreground restricted apps and blocks them.",
                icon = Icons.Default.AccessibilityNew,
                isGranted = isAccessibilityEnabled,
                isRequired = true,
                actionLabel = "Enable",
                onAction = onEnableAccessibility
            )

            ChecklistRow(
                title = "Display over other apps",
                description = "Shows the block screen overlay above distracting apps.",
                icon = Icons.Default.Layers,
                isGranted = isOverlayEnabled,
                isRequired = true,
                actionLabel = "Enable",
                onAction = onEnableOverlay
            )

            ChecklistRow(
                title = "Battery optimization",
                description = "Prevents Android from putting the background guard to sleep.",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = isBatteryIgnored,
                isRequired = true,
                actionLabel = "Enable",
                onAction = onEnableBattery
            )

            ChecklistRow(
                title = "App PIN",
                description = "Optional 4-digit PIN required to cancel focus sessions.",
                icon = Icons.Default.Lock,
                isGranted = isPinConfigured,
                isRequired = false,
                actionLabel = if (isPinConfigured) "Change" else "Set PIN",
                grantedLabel = "Configured",
                onAction = { showPinDialog = true }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Completion Action Button
            FocusButton(
                text = if (allRequiredGranted) "Complete setup" else "Complete required items ($grantedCount/$requiredCount)",
                onClick = onComplete,
                enabled = allRequiredGranted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onSavePin = { pin ->
                onSetPin(pin)
                showPinDialog = false
            }
        )
    }
}

/**
 * Standardized compact checklist row.
 * Uniform layout in all states:
 * [Icon] -> [Column with Title & full-width description] -> [Status Badge or Action Button]
 * Height derived purely from content, never stretched.
 */
@Composable
private fun ChecklistRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    isRequired: Boolean,
    actionLabel: String,
    grantedLabel: String = "Enabled",
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors

    Surface(
        shape = FocusShapes.medium,
        color = colors.surface,
        border = BorderStroke(
            1.dp,
            if (isGranted) colors.statusActive.copy(alpha = 0.3f) else colors.border
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon (34dp)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) colors.statusActive.copy(alpha = 0.12f)
                        else colors.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) colors.statusActive else colors.textPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Title & Full-width Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = description,
                    style = FocusTypography.caption.copy(fontSize = 12.sp, lineHeight = 15.sp),
                    color = colors.textSecondary
                )
            }

            // Status Badge (when granted) OR Action Button (when not granted)
            if (isGranted) {
                Surface(
                    shape = FocusShapes.small,
                    color = colors.statusActive.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = grantedLabel,
                        color = colors.statusActive,
                        style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                FocusButton(
                    text = actionLabel,
                    onClick = onAction
                )
            }
        }
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    val colors = FocusTheme.colors
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Set app PIN",
                style = FocusTypography.title,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm)) {
                Text(
                    text = "Enter a 4-digit PIN to prevent impulsively stopping focus sessions.",
                    style = FocusTypography.bodySmall,
                    color = colors.textSecondary
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it.filter { c -> c.isDigit() } },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = colors.statusDestructive,
                        style = FocusTypography.caption
                    )
                }
            }
        },
        confirmButton = {
            FocusButton(
                text = "Save PIN",
                onClick = {
                    if (pin.length != 4) {
                        error = "PIN must be 4 digits"
                    } else if (pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onSavePin(pin)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
