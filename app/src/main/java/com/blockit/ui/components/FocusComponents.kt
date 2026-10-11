package com.blockit.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

/**
 * Clean 56dp Top App Bar with Option B Monolith mark and unified typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    showLogo: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = FocusTheme.colors
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
            ) {
                if (showLogo) {
                    FocusMonolithMark(
                        size = 22.dp,
                        tint = colors.accent
                    )
                }
                Text(
                    text = title,
                    style = FocusTypography.title,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = colors.textPrimary
                    )
                }
            } else if (onMenuClick != null) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Open navigation menu",
                        tint = colors.textPrimary
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            scrolledContainerColor = colors.surface,
            navigationIconContentColor = colors.textPrimary,
            titleContentColor = colors.textPrimary,
            actionIconContentColor = colors.textPrimary
        ),
        modifier = modifier
    )
}

/**
 * Flat 64dp Material 3 Bottom Navigation Bar (replaces floating pill).
 */
@Composable
fun FocusNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    val destinations = listOf(
        NavBarDestination("home", "Home", Icons.Rounded.Home),
        NavBarDestination("profiles", "Profiles", Icons.Rounded.Layers),
        NavBarDestination("stats", "Stats", Icons.Rounded.BarChart)
    )

    NavigationBar(
        modifier = modifier.height(64.dp),
        containerColor = colors.surface,
        tonalElevation = 0.dp
    ) {
        destinations.forEach { dest ->
            val isSelected = currentRoute == dest.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(dest.route) },
                icon = {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = FocusTypography.label
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    unselectedIconColor = colors.textTertiary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.accentContainer
                )
            )
        }
    }
}

private data class NavBarDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Standard list row with 48dp minimum touch target.
 */
@Composable
fun FocusListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = FocusTheme.colors
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(clickableModifier)
            .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()
            }
            Spacer(modifier = Modifier.width(FocusSpacing.md))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = FocusTypography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FocusTypography.caption,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(FocusSpacing.sm))
            trailingContent()
        }
    }
}

/**
 * Flat surface switch row with 1dp border and min 48dp touch target.
 */
@Composable
fun FocusSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val colors = FocusTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(FocusShapes.medium)
            .border(1.dp, colors.border, FocusShapes.medium)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        color = colors.surface,
        shape = FocusShapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = FocusTypography.bodyMedium,
                    color = if (enabled) colors.textPrimary else colors.textTertiary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = FocusTypography.caption,
                        color = if (enabled) colors.textSecondary else colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(FocusSpacing.md))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textTertiary,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.border
                )
            )
        }
    }
}

/**
 * Clean Section Header with sentence case.
 */
@Composable
fun FocusSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = FocusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FocusTypography.title,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(FocusSpacing.sm))
            action()
        }
    }
}

/**
 * Primary CTA Button with 8dp radius and 48dp height.
 */
@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = FocusTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = FocusShapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm),
        modifier = modifier
            .heightIn(min = 48.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(FocusSpacing.xs))
        }
        Text(
            text = text,
            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Secondary / Outlined Button.
 */
@Composable
fun FocusOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = FocusTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = FocusShapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textTertiary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.border)
        ),
        contentPadding = PaddingValues(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(FocusSpacing.xs))
        }
        Text(
            text = text,
            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Destructive Action Button.
 */
@Composable
fun FocusDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = FocusTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = FocusShapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.statusDestructive,
            contentColor = Color.White,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(FocusSpacing.xs))
        }
        Text(
            text = text,
            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Calm Active Session Card (replaces the giant timer home screen).
 * Shows profile name & mode, a clear countdown timer with progress ring, and friction Stop action.
 */
@Composable
fun FocusActiveSessionCard(
    profileName: String,
    isWhitelist: Boolean,
    remainingSeconds: Long,
    totalSeconds: Long,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false
) {
    val colors = FocusTheme.colors
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    val timeString = if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "session_progress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(FocusShapes.medium)
            .border(1.dp, colors.border, FocusShapes.medium),
        color = colors.surface,
        shape = FocusShapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Status indicator + Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) colors.warning else colors.statusActive)
                    )
                    Text(
                        text = if (isPaused) "Paused (Break)" else "Active session",
                        style = FocusTypography.label,
                        color = if (isPaused) colors.warning else colors.statusActive
                    )
                }

                Surface(
                    color = colors.surfaceVariant,
                    shape = FocusShapes.small
                ) {
                    Text(
                        text = if (isWhitelist) "Allowlist" else "Blocklist",
                        style = FocusTypography.label,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.md))

            // Profile name
            Text(
                text = profileName,
                style = FocusTypography.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FocusSpacing.lg))

            // Circular progress ring + Countdown text
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(160.dp),
                    color = colors.surfaceVariant,
                    strokeWidth = 6.dp,
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(160.dp),
                    color = if (isPaused) colors.warning else colors.accent,
                    strokeWidth = 6.dp,
                    trackColor = Color.Transparent
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        style = FocusTypography.display,
                        color = colors.textPrimary
                    )
                    if (isPaused) {
                        Text(
                            text = "PAUSED",
                            style = FocusTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = colors.warning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.xl))

            // Friction Stop button
            FocusDestructiveButton(
                text = "Stop session",
                onClick = onStopClick,
                leadingIcon = Icons.Rounded.Stop,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Standard Bottom Sheet with 20dp top rounded corners and drag handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = FocusTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = FocusShapes.large,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = FocusSpacing.sm)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        },
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Calm Empty State component.
 */
@Composable
fun FocusEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionButton: (@Composable () -> Unit)? = null
) {
    val colors = FocusTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FocusSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(FocusSpacing.md))
        }

        Text(
            text = title,
            style = FocusTypography.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FocusSpacing.xs))

        Text(
            text = message,
            style = FocusTypography.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (actionButton != null) {
            Spacer(modifier = Modifier.height(FocusSpacing.lg))
            actionButton()
        }
    }
}

/**
 * Standard Alert Dialog with 20dp corner radius and high contrast.
 */
@Composable
fun FocusAlertDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = FocusTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FocusShapes.large,
        containerColor = colors.surface,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) colors.statusDestructive else colors.accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else null,
        title = {
            Text(
                text = title,
                style = FocusTypography.title,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = text,
                style = FocusTypography.body,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            if (isDestructive) {
                FocusDestructiveButton(text = confirmText, onClick = onConfirm)
            } else {
                FocusButton(text = confirmText, onClick = onConfirm)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = FocusShapes.small
            ) {
                Text(
                    text = dismissText,
                    style = FocusTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Clean system app icon loader with calm fallback.
 */
@Composable
fun FocusAppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    appName: String? = null,
    size: Dp = 36.dp
) {
    val colors = FocusTheme.colors
    val iconBitmapState = com.blockit.core.cache.rememberAsyncAppIcon(packageName)
    val iconBitmap = iconBitmapState.value

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(FocusShapes.small)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(FocusShapes.small)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val initial = (appName?.take(1) ?: packageName.takeLast(1)).uppercase()
            Text(
                text = initial,
                style = FocusTypography.label,
                color = colors.textSecondary
            )
        }
    }
}
