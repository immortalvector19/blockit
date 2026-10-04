package com.blockit.ui.overlay

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.ui.theme.BlockItTheme

@Composable
fun BlockScreenContent(
    targetIdentifier: String,
    profileName: String,
    reason: String,
    isWhitelistBlock: Boolean = false,
    allowedApps: List<String> = emptyList(),
    onHomeClicked: () -> Unit,
    onOpenAllowedApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrompt by remember { mutableStateOf(false) }

    BlockItTheme(darkTheme = true) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color(0xFF23211F) // Authentic BlockIt dark charcoal machine body
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mode Icon
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(if (isWhitelistBlock) Color(0xFF1E3A2F) else Color(0xFF383532)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWhitelistBlock) Icons.Default.Lock else Icons.Default.SelfImprovement,
                        contentDescription = if (isWhitelistBlock) "Whitelist Locked" else "Focus",
                        tint = if (isWhitelistBlock) Color(0xFF22C55E) else Color(0xFFFF5520),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isWhitelistBlock) "Whitelist Mode Active" else "Pause & Reset",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFFF4F3EF)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isWhitelistBlock) "Only permitted apps can be used right now" else "Blocked by '$profileName'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFA8A49C),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detail card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF33302D))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWhitelistBlock) "Access Restricted" else "Rule Triggered",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isWhitelistBlock) "Attempted: $targetIdentifier" else targetIdentifier,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFF8FAFC)
                        )
                        if (reason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // If Whitelist Mode, show permitted apps launcher
                if (isWhitelistBlock && allowedApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Permitted Apps (Tap to Open):",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF34D399),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allowedApps) { pkg ->
                            val appName = remember(pkg) { getAppName(context, pkg) }
                            Card(
                                onClick = {
                                    if (onOpenAllowedApp != null) {
                                        onOpenAllowedApp(pkg)
                                    } else {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                                        launchIntent?.let { context.startActivity(it) }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFFF8FAFC)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isWhitelistBlock) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // Optional "Why am I here?" reflective toggle
                    OutlinedButton(
                        onClick = { showPrompt = !showPrompt },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (showPrompt) "Hide Reflection" else "Why am I here?",
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    AnimatedVisibility(visible = showPrompt) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Was opening this an intentional choice or an unconscious reflex?\nTake three deep breaths before deciding what to do next.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Action: Return to Home
                Button(
                    onClick = onHomeClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWhitelistBlock) Color(0xFF16A34A) else com.blockit.ui.theme.BlockitOrange,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isWhitelistBlock) "Focus Mode Active" else "Return to Home Screen",
                        style = MaterialTheme.typography.titleMedium
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
