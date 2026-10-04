package com.blockit.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.data.local.datastore.PreferenceManager
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Step Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                (0..2).forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (currentStep == index) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (currentStep == index) Color(0xFF818CF8)
                                else Color(0xFF334155)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> WelcomeStepContent()
                    1 -> BlockingModesStepContent()
                    2 -> PermissionsStepContent(context = context)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back", color = Color(0xFF94A3B8))
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                if (currentStep < 2) {
                    Button(
                        onClick = { currentStep++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Continue")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                preferenceManager.setOnboardingCompleted(true)
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish & Start Focusing")
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to BlockIt",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF1F5F9),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A private, 100% on-device blocker engineered to eliminate digital distractions without ads, accounts, or telemetry.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                BulletPoint(title = "Zero Internet Usage", description = "No internet permissions requested. Your data never leaves your device.")
                Spacer(modifier = Modifier.height(14.dp))
                BulletPoint(title = "Sub-Millisecond Interception", description = "Catches distracting apps and website domains instantly before habits take over.")
                Spacer(modifier = Modifier.height(14.dp))
                BulletPoint(title = "Unbreakable Strict Mode", description = "Optional friction challenges and device protection to keep you accountable.")
            }
        }
    }
}

@Composable
private fun BlockingModesStepContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Two Ways to Block",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF1F5F9),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "BlockIt supports both Blacklist and Whitelist modes for different focus needs:",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Blacklist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("1. Blacklist Mode (Default)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF8FAFC))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You can use your phone normally. Only the specific apps and websites you add to your blocklist will be intercepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Whitelist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("2. Whitelist Mode (Strict Kiosk)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF8FAFC))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Locks down your device! Only the few permitted apps you select (e.g. Phone, Notes) can be opened. All other apps and home screen distractions are blocked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun PermissionsStepContent(context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityGranted by remember { mutableStateOf(checkAccessibility(context)) }
    var isOverlayGranted by remember { mutableStateOf(checkOverlay(context)) }
    var isVpnGranted by remember { mutableStateOf(checkVpn(context)) }

    fun refresh() {
        isAccessibilityGranted = checkAccessibility(context)
        isOverlayGranted = checkOverlay(context)
        isVpnGranted = checkVpn(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refresh()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Enable Core Permissions",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF1F5F9),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Because BlockIt is sideloaded for personal use, it requires these 2 essential Android permissions to detect and block apps:",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Accessibility Service
        OnboardingPermissionCard(
            title = "1. Accessibility Service (Required)",
            description = "Allows BlockIt to detect when a distracting app opens in the foreground.",
            isGranted = isAccessibilityGranted,
            icon = Icons.Default.AccessibilityNew,
            buttonText = "Turn ON in Settings",
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Overlay Permission
        OnboardingPermissionCard(
            title = "2. Display Over Other Apps (Required)",
            description = "Allows BlockIt to display the calm full-screen blocking screen over distracting apps.",
            isGranted = isOverlayGranted,
            icon = Icons.Default.Layers,
            buttonText = "Grant Overlay Permission",
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Local VPN Filter
        OnboardingPermissionCard(
            title = "3. On-Device DNS Filter (Websites)",
            description = "Runs local DNS filtering to block website domains across browsers without sending traffic off-device.",
            isGranted = isVpnGranted,
            icon = Icons.Default.VpnKey,
            buttonText = "Authorize Local VPN",
            onClick = {
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    vpnLauncher.launch(intent)
                } else {
                    refresh()
                }
            }
        )
    }
}

@Composable
private fun OnboardingPermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF1E293B) else Color(0xFF291B24)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF34D399) else Color(0xFFF87171),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Granted" else "Pending",
                    tint = if (isGranted) Color(0xFF34D399) else Color(0xFFF87171),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF818CF8))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF1F5F9))
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        }
    }
}

private fun checkAccessibility(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val expected = "${context.packageName}/com.blockit.service.accessibility.BlockAccessibilityService"
    return enabledServices.contains(expected)
}

private fun checkOverlay(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

private fun checkVpn(context: Context): Boolean {
    return VpnService.prepare(context) == null
}
