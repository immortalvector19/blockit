package com.blockit.ui.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.ui.theme.BlockItTheme

/**
 * FocusShield Intent Gate Overlay
 * Replaces plain block walls with a thoughtful gate asking "What for?"
 * Offers time-boxed 3-minute passes (limited per day) unless Hard Lock is active.
 */
@Composable
fun BlockScreenContent(
    targetIdentifier: String,
    profileName: String,
    reason: String,
    isWhitelistBlock: Boolean = false,
    allowedApps: List<String> = emptyList(),
    isHardLock: Boolean = false,
    canRequestPass: Boolean = false,
    remainingPasses: Int = 0,
    onHomeClicked: () -> Unit,
    onOpenAllowedApp: ((String) -> Unit)? = null,
    onGrantPass: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetAppName = remember(targetIdentifier) { getAppName(context, targetIdentifier) }
    val targetAppIcon = remember(targetIdentifier) { getAppIcon(context, targetIdentifier) }

    // Intent Gate State
    var selectedIntent by remember { mutableStateOf<String?>(null) }
    val intentOptions = listOf(
        "Quick lookup",
        "Urgent reply",
        "Work utility",
        "Unconscious reflex"
    )

    // Palette: ink background, elevated surfaces, signal accent
    val bgInk = Color(0xFF0E1621)
    val surfaceElevated = Color(0xFF172233)
    val borderDim = Color(0xFF283952)
    val signalGreen = Color(0xFF3DDC97)
    val textPrimary = Color(0xFFF1F5F9)
    val textMuted = Color(0xFF94A3B8)
    val alertAmber = Color(0xFFF59E0B)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = bgInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Shield Identity Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(surfaceElevated)
                        .border(1.dp, borderDim, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = signalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FocusShield: $profileName",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (isWhitelistBlock) {
                    // -------------------------------------------------------------
                    // WHITELIST KIOSK GATE
                    // -------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(signalGreen.copy(alpha = 0.15f))
                            .border(1.dp, signalGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = signalGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Whitelist Mode Active",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Only permitted applications are accessible right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (allowedApps.isNotEmpty()) {
                        Text(
                            text = "Permitted Apps",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allowedApps) { pkg ->
                                val appName = remember(pkg) { getAppName(context, pkg) }
                                val appIcon = remember(pkg) { getAppIcon(context, pkg) }
                                Surface(
                                    onClick = {
                                        if (onOpenAllowedApp != null) {
                                            onOpenAllowedApp(pkg)
                                        } else {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                                            launchIntent?.let {
                                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                                context.startActivity(it)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = surfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderDim)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (appIcon != null) {
                                            Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(22.dp))
                                        } else {
                                            Icon(Icons.Default.Apps, contentDescription = null, tint = signalGreen, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = appName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // -------------------------------------------------------------
                    // INTENT GATE (ORIGINAL BLACKLIST PASS SYSTEM)
                    // -------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceElevated)
                            .border(1.dp, borderDim, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetAppIcon != null) {
                            Image(
                                bitmap = targetAppIcon,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = alertAmber,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pause: What are you opening $targetAppName for?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Notice the trigger before proceeding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Intent Option Chips
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intentOptions.forEach { option ->
                            val isSelected = selectedIntent == option
                            Surface(
                                onClick = { selectedIntent = option },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) signalGreen.copy(alpha = 0.12f) else surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) signalGreen else borderDim
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) signalGreen else textPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = signalGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Response based on selected intention
                    if (selectedIntent == "Unconscious reflex") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderDim)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Great awareness.",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = signalGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Take 3 slow breaths. Notice the impulse fade away, then return to what you were doing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textMuted
                                )
                            }
                        }
                    } else if (selectedIntent != null) {
                        if (isHardLock) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF291B24)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Hard Lock Mode Active. No emergency passes are permitted for this profile.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }
                        } else if (canRequestPass && remainingPasses > 0 && onGrantPass != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderDim)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Time-Boxed Emergency Pass",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "$remainingPasses left today",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = alertAmber
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You will receive 3 minutes to handle your business. Once expired, you will be automatically returned to Home.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textMuted
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onGrantPass,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = signalGreen,
                                            contentColor = Color(0xFF0E1621)
                                        )
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Start 3-Minute Pass",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderDim)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Daily Pass Limit Reached",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = alertAmber
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You have used all 3 emergency passes for today. Stay focused on your primary task.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Primary Exit Action: Always Home Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onHomeClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedIntent == "Unconscious reflex") signalGreen else surfaceElevated,
                        contentColor = if (selectedIntent == "Unconscious reflex") Color(0xFF0E1621) else textPrimary
                    ),
                    border = if (selectedIntent != "Unconscious reflex") androidx.compose.foundation.BorderStroke(1.dp, borderDim) else null
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedIntent == "Unconscious reflex") "Put Phone Down (Go Home)" else "Return to Home Screen",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private fun getAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName.substringAfterLast('.')
    }
}

private fun getAppIcon(context: Context, packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
