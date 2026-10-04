package com.blockit.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.ui.theme.BlockitTheme
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    preferenceManager: PreferenceManager,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BlockitTheme.colors
    val haptic = LocalHapticFeedback.current

    var currentStep by remember { mutableIntStateOf(0) }
    var showAccessibilityPreDialog by remember { mutableStateOf(false) }

    // Live permission states
    var hasNotification by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    var hasOverlay by remember { mutableStateOf(checkOverlayPermission(context)) }
    var hasBattery by remember { mutableStateOf(checkBatteryOptimization(context)) }

    fun refreshPermissions() {
        hasNotification = checkNotificationPermission(context)
        hasAccessibility = checkAccessibilityPermission(context)
        hasOverlay = checkOverlayPermission(context)
        hasBattery = checkBatteryOptimization(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        refreshPermissions()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress & Brand Identity
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = 4,
                onStepClick = { target ->
                    if (target < currentStep) currentStep = target
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content with smooth horizontal transition
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(280)
                        ) + fadeIn(tween(280))).togetherWith(
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(280)
                            ) + fadeOut(tween(200))
                        )
                    } else {
                        (slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(280)
                        ) + fadeIn(tween(280))).togetherWith(
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(280)
                            ) + fadeOut(tween(200))
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_step_content"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentStep = 1
                        }
                    )
                    1 -> CoreEnginesStep(
                        hasAccessibility = hasAccessibility,
                        hasOverlay = hasOverlay,
                        onRequestAccessibility = { showAccessibilityPreDialog = true },
                        onRequestOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onBack = { currentStep = 0 },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentStep = 2
                        }
                    )
                    2 -> ReliabilityStep(
                        hasBattery = hasBattery,
                        hasNotification = hasNotification,
                        onRequestBattery = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                hasNotification = true
                            }
                        },
                        onBack = { currentStep = 1 },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentStep = 3
                        }
                    )
                    else -> ReadyStep(
                        hasAccessibility = hasAccessibility,
                        hasOverlay = hasOverlay,
                        hasBattery = hasBattery,
                        onComplete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                preferenceManager.setOnboardingCompleted(true)
                                onComplete()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Privacy Guarantee
            Text(
                text = "100% On-Device • Zero Network • No Telemetry",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }

        // Accessibility Pre-Grant Explanation Dialog
        if (showAccessibilityPreDialog) {
            AlertDialog(
                onDismissRequest = { showAccessibilityPreDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "Enable App Detection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        "On the next screen, find 'FocusShield' under Downloaded/Installed Services and switch it ON.\n\n" +
                        "Why it is needed: FocusShield uses this to detect when a blocked app is opened and redirect you back to your focus session.\n\n" +
                        "Privacy promise: FocusShield runs entirely locally on this device. It never reads keystrokes, personal messages, or screen data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAccessibilityPreDialog = false
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccessibilityPreDialog = false }) {
                        Text("Cancel", color = colors.textMuted)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    onStepClick: (Int) -> Unit
) {
    val colors = BlockitTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Brand Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FocusShield",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colors.textPrimary
                )
            }

            // Step Indicator Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text(
                    text = "${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Progress Bar (Apple-style subtle bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { index ->
                val isCompleted = index <= currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isCompleted) colors.primary else colors.border)
                        .clickable { onStepClick(index) }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    val colors = BlockitTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic FocusShield Iris Emblem
            FocusShieldEmblem(
                isArmed = false,
                accentColor = colors.primary,
                bodyColor = colors.surfaceVariant,
                strokeColor = colors.border
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Reclaim Your Focus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A calm personal defense system against digital distraction. Built with strict privacy and zero cloud compromises.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Core Pillars Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillarCard(
                    icon = Icons.Default.Security,
                    title = "100% On-Device Protection",
                    description = "No network calls, no analytics, no accounts. Your blocking rules never leave your phone."
                )
                PillarCard(
                    icon = Icons.Default.Speed,
                    title = "Kernel-Grade Defense",
                    description = "Sideload-optimized architecture using Android's most reliable system APIs for unbreakable blocking."
                )
                PillarCard(
                    icon = Icons.Default.Lock,
                    title = "Custom Modes & PIN Lock",
                    description = "Tailor blacklist or whitelist profiles, recurring schedules, and prevent impulsive cancellation."
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CTA Button
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Begin Setup",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CoreEnginesStep(
    hasAccessibility: Boolean,
    hasOverlay: Boolean,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = BlockitTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Core Defense Engines",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "FocusShield needs these two capabilities to detect distracting apps and render full-screen shield overlays.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permission Card 1: Accessibility
            PermissionSetupCard(
                icon = Icons.Default.AccessibilityNew,
                title = "App Detection Engine",
                subtitle = "Accessibility Service",
                isGranted = hasAccessibility,
                description = "Detects when blocked apps are launched and instantly redirects you to your focus shield. Never reads screen content, keystrokes, or messages.",
                actionText = "Enable in Settings",
                onAction = onRequestAccessibility
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Card 2: Overlay
            PermissionSetupCard(
                icon = Icons.Default.Layers,
                title = "Full-Screen Shield Overlay",
                subtitle = "Display Over Other Apps",
                isGranted = hasOverlay,
                description = "Allows FocusShield to immediately show the calm full-screen blocking card over restricted apps without delay.",
                actionText = "Grant Overlay",
                onAction = onRequestOverlay
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("Back", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasAccessibility && hasOverlay) colors.primary else colors.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (hasAccessibility && hasOverlay) "Continue →" else "Next Step",
                    color = if (hasAccessibility && hasOverlay) Color.White else colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReliabilityStep(
    hasBattery: Boolean,
    hasNotification: Boolean,
    onRequestBattery: () -> Unit,
    onRequestNotification: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = BlockitTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Background Reliability",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Prevent aggressive system battery savers from stopping your active focus sessions.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permission Card 3: Battery Optimization
            PermissionSetupCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Ignore Battery Optimizations",
                subtitle = "Survive OEM Memory Killers",
                isGranted = hasBattery,
                description = "Exempts FocusShield from aggressive background sleep routines on Samsung, Xiaomi, and Pixel devices so active blocks never fail.",
                actionText = "Exempt App",
                onAction = onRequestBattery
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Card 4: Notifications
            PermissionSetupCard(
                icon = Icons.Default.Notifications,
                title = "Active Session Alerts",
                subtitle = "Countdown Notifications",
                isGranted = hasNotification,
                description = "Shows quiet, persistent countdown notifications during active focus blocks so you know when sessions end.",
                actionText = "Allow Notifications",
                onAction = onRequestNotification
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("Back", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Continue →",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReadyStep(
    hasAccessibility: Boolean,
    hasOverlay: Boolean,
    hasBattery: Boolean,
    onComplete: () -> Unit
) {
    val colors = BlockitTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Armed FocusShield Emblem
            FocusShieldEmblem(
                isArmed = true,
                accentColor = Color(0xFF22C55E),
                bodyColor = colors.surfaceVariant,
                strokeColor = colors.border
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "FocusShield is Armed",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your phone is now equipped with tactical distraction defense. All rules run 100% locally on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Engine Status Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusCheckRow(
                        title = "App Detection Engine",
                        isActive = hasAccessibility
                    )
                    StatusCheckRow(
                        title = "Full-Screen Shield Overlay",
                        isActive = hasOverlay
                    )
                    StatusCheckRow(
                        title = "Background Memory Guardian",
                        isActive = hasBattery
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pro-tip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tip: You can set a 4-digit security PIN lock in Settings > Strict Mode to prevent yourself from canceling active focus blocks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Final Launch Button
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Enter FocusShield",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun PillarCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = BlockitTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionSetupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    val colors = BlockitTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) colors.surface else colors.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) Color(0xFF22C55E).copy(alpha = 0.4f) else colors.border
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isGranted) Color(0xFF22C55E).copy(alpha = 0.15f)
                                else colors.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF22C55E) else colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGranted) Color(0xFF166534).copy(alpha = 0.15f) else Color(0xFF9A3412).copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (isGranted) Color(0xFF22C55E).copy(alpha = 0.3f) else Color(0xFFEA580C).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isGranted) Color(0xFF22C55E) else Color(0xFFEA580C))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isGranted) "Granted ✓" else "Action Req.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isGranted) Color(0xFF22C55E) else Color(0xFFEA580C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) Color(0xFF22C55E) else colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isGranted) "Active ✓" else actionText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatusCheckRow(
    title: String,
    isActive: Boolean
) {
    val colors = BlockitTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isActive) Color(0xFF22C55E) else colors.textMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) "Ready" else "Optional",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) Color(0xFF22C55E) else colors.textMuted
            )
        }
    }
}

/**
 * High-craft FocusShield Emblem with concentric aperture rings and glowing focus core.
 */
@Composable
private fun FocusShieldEmblem(
    isArmed: Boolean,
    accentColor: Color,
    bodyColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 4f
            val midRadius = outerRadius * 0.72f
            val innerRadius = outerRadius * 0.42f

            // Outer Concentric Track
            drawCircle(
                color = strokeColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Mid Track
            drawCircle(
                color = if (isArmed) accentColor.copy(alpha = 0.3f) else strokeColor,
                radius = midRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Inner Armor Plate Fill
            drawCircle(
                color = bodyColor,
                radius = midRadius,
                center = center
            )

            // Core Glow Ring
            drawCircle(
                color = accentColor.copy(alpha = 0.2f),
                radius = innerRadius * 1.4f,
                center = center
            )

            // Core Focal Jewel
            drawCircle(
                color = accentColor,
                radius = innerRadius * 0.7f,
                center = center
            )
        }

        // Center Emblem Icon
        Icon(
            imageVector = if (isArmed) Icons.Default.CheckCircle else Icons.Default.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkAccessibilityPermission(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val expected = "${context.packageName}/com.blockit.service.accessibility.BlockAccessibilityService"
    return enabledServices.contains(expected)
}

private fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
