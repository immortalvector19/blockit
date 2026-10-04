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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.blockit.ui.components.AnalogLever
import com.blockit.ui.components.BlockNumberDisplay
import com.blockit.ui.components.BlockitProgressSquares
import com.blockit.ui.components.SlideToActButton
import com.blockit.ui.theme.BlockitOrange
import com.blockit.ui.theme.CharcoalDark
import com.blockit.ui.theme.CreamBackground
import com.blockit.ui.theme.CreamSurface
import com.blockit.ui.theme.CreamText
import com.blockit.ui.theme.CreamTextMuted
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    preferenceManager: PreferenceManager,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        if (granted && currentStep == 2) currentStep = 3
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: lowercase blockit title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "blockit",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = CharcoalDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "blockit_onboarding_step"
            ) { step ->
                when (step) {
                    0 -> StepAnalog(onPullLever = { currentStep = 1 })
                    1 -> StepBeforeBoarding(
                        grantedCount = listOf(hasNotification, hasAccessibility, hasOverlay, hasBattery).count { it },
                        onPullLever = { currentStep = 2 }
                    )
                    2 -> StepPermissionCard(
                        digit = 1,
                        activeStepNumber = 1,
                        isGranted = hasNotification,
                        title = "NOTIFICATIONS",
                        description = "Grant it to alert you when a session is active and how much time is left until the block ends.",
                        bullet = "Only for the block: no sound, no vibration, no ads.",
                        buttonText = if (hasNotification) "Granted" else "Grant",
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                hasNotification = true
                                currentStep = 3
                            }
                        },
                        onPullLever = { currentStep = 3 }
                    )
                    3 -> StepPermissionCard(
                        digit = 2,
                        activeStepNumber = 2,
                        isGranted = hasAccessibility,
                        title = "ACCESSIBILITY",
                        description = "Turns the screen off if you leave to an app that isn't allowed during a session. No data is collected.",
                        bullet = "blockit never reads what is on your screen: it only brings you back here.",
                        buttonText = if (hasAccessibility) "Granted" else "Grant",
                        onGrant = {
                            showAccessibilityPreDialog = true
                        },
                        onPullLever = { currentStep = 4 }
                    )
                    4 -> StepPermissionCard(
                        digit = 3,
                        activeStepNumber = 3,
                        isGranted = hasOverlay,
                        title = "SHOW OVER APPS",
                        description = "It lets blockit bring you back to the session, and show the time left above the apps you allowed.",
                        bullet = "Without it the block screen may fail to come back on its own.",
                        buttonText = if (hasOverlay) "Granted" else "Grant",
                        onGrant = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onPullLever = { currentStep = 5 }
                    )
                    5 -> StepPermissionCard(
                        digit = 4,
                        activeStepNumber = 4,
                        isGranted = hasBattery,
                        title = "BATTERY",
                        description = "Turns off battery optimization for blockit, so the system can't interrupt an ongoing block.",
                        bullet = "The system may otherwise stop a block that is already running.",
                        buttonText = if (hasBattery) "Granted" else "Grant",
                        onGrant = {
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
                        onPullLever = { currentStep = 6 }
                    )
                    else -> StepMachineReady(
                        onSlideToStart = {
                            scope.launch {
                                preferenceManager.setOnboardingCompleted(true)
                                onComplete()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "100% On-Device • Zero Internet • No Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = CreamTextMuted
                )
            }
        }

        // Accessibility Pre-Grant Dialog (matching video frame 00:12)
        if (showAccessibilityPreDialog) {
            AlertDialog(
                onDismissRequest = { showAccessibilityPreDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = BlockitOrange,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "Enable session block",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalDark
                    )
                },
                text = {
                    Text(
                        "During a session, if you open an app that isn't on your whitelist, blockit uses Android's Accessibility Service to bring you back to the block screen. It doesn't read any screen content and doesn't collect or share any data.\n\nWithout this service, blockit can't block your phone. On the next screen, turn on 'blockit - Session block'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CreamText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAccessibilityPreDialog = false
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalDark)
                    ) {
                        Text("I agree")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccessibilityPreDialog = false }) {
                        Text("Not now", color = CreamTextMuted)
                    }
                },
                containerColor = CreamBackground,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun StepAnalog(onPullLever: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Center Orange Block graphic
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BlockitOrange)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = "▶  ANALOG",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = CharcoalDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "blockit works like a machine: wheels, levers and handles, each with its own click. The lever on the right moves you forward. Pull it down until it clicks to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamTextMuted,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(BlockitOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "The lever clicks when the gesture is done: you feel it in your hand.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamTextMuted
                    )
                }
            }

            AnalogLever(onTrigger = onPullLever)
        }

        Spacer(modifier = Modifier.height(28.dp))

        TextButton(onClick = onPullLever) {
            Text(
                "PULL THE LEVER OR TAP HERE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = CreamTextMuted
            )
        }
    }
}

@Composable
private fun StepBeforeBoarding(
    grantedCount: Int,
    onPullLever: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Geometric arrangement of blocks matching video frame 00:04
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(BlockitOrange))
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFCCBC)))
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFD6D4CC)))
        }

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = "🔒  BEFORE BOARDING",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = CharcoalDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "To really lock your phone, blockit needs 4 system permissions. I will ask for them one at a time: on some phones they are well hidden, and without them the block does not hold.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamTextMuted,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(BlockitOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (grantedCount > 0) "$grantedCount of 4 permissions already granted." else "Nothing is granted yet: they light up one by one as you go.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamTextMuted
                    )
                }
            }

            AnalogLever(onTrigger = onPullLever)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onPullLever,
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalDark),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Begin Setup", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepPermissionCard(
    digit: Int,
    activeStepNumber: Int,
    isGranted: Boolean,
    title: String,
    description: String,
    bullet: String,
    buttonText: String,
    onGrant: () -> Unit,
    onPullLever: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big Block Number: 01, 02, 03, 04
        BlockNumberDisplay(number = digit, color = CharcoalDark)

        Spacer(modifier = Modifier.height(8.dp))

        // Progress squares matching video: [■] [□] [□] [□]
        BlockitProgressSquares(currentStep = activeStepNumber, totalSteps = 4)

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = CharcoalDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamTextMuted,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(BlockitOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamTextMuted
                    )
                }
            }

            AnalogLever(onTrigger = onPullLever)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF22C55E) else CharcoalDark
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isGranted) "Granted ✓" else buttonText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun StepMachineReady(
    onSlideToStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // All 4 squares illuminated
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(6.dp)).background(BlockitOrange))
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "THE MACHINE IS READY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 1.sp
            ),
            color = CharcoalDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Your phone is now equipped with tactile focus controls. All rules run 100% locally on this device with zero accounts, zero analytics, and zero telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            color = CreamTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Slide to start button matching video frame 00:14
        SlideToActButton(
            text = "Slide to Start",
            isActive = false,
            onTrigger = onSlideToStart
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
