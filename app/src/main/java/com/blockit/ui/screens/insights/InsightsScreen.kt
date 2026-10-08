package com.blockit.ui.screens.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.domain.model.TargetStatItem
import com.blockit.ui.components.FocusAlertDialog
import com.blockit.ui.components.FocusAppIcon
import com.blockit.ui.components.FocusEmptyState
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            FocusTopBar(
                title = "Usage insights",
                onBackClick = onBackClick,
                actions = {
                    if (uiState.totalBlocked > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear history",
                                tint = colors.statusDestructive
                            )
                        }
                    }
                }
            )
        },
        containerColor = colors.background
    ) { padding ->
        if (uiState.totalBlocked == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                FocusEmptyState(
                    title = "No distractions blocked yet",
                    message = "When FocusShield blocks apps or websites matching your active profiles, your time saved and distraction trends will appear here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FocusSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
            ) {
                // Hero card: Time Saved
                item {
                    TimeReclaimedHeroCard(
                        formattedTimeSaved = uiState.formattedTimeSaved,
                        totalBlocked = uiState.totalBlocked
                    )
                }

                // 3 Stats Metric Cards
                item {
                    MetricsRow(
                        todayCount = uiState.blockedToday,
                        weekCount = uiState.blockedThisWeek,
                        allTimeCount = uiState.totalBlocked
                    )
                }

                // Top Blocked Targets
                if (uiState.topTargets.isNotEmpty()) {
                    item {
                        FocusSectionHeader(title = "Most blocked distractions")
                    }

                    items(uiState.topTargets, key = { "${it.targetType}_${it.identifier}" }) { item ->
                        val maxCount = uiState.topTargets.maxOfOrNull { it.count } ?: 1
                        TopTargetItemCard(item = item, maxCount = maxCount)
                    }
                }

                // Recent Activity
                if (uiState.recentEvents.isNotEmpty()) {
                    item {
                        FocusSectionHeader(title = "Recent activity")
                    }

                    items(uiState.recentEvents.take(20), key = { it.id }) { event ->
                        RecentEventItemCard(event = event)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        FocusAlertDialog(
            title = "Clear audit history?",
            text = "This will reset all blocked attempt statistics and recent activity history. This cannot be undone.",
            confirmText = "Clear",
            onConfirm = {
                viewModel.clearHistory()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
            isDestructive = true
        )
    }
}

@Composable
private fun TimeReclaimedHeroCard(
    formattedTimeSaved: String,
    totalBlocked: Int
) {
    val colors = FocusTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = FocusShapes.medium,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.lg)) {
            Text(
                text = "Focus time saved",
                style = FocusTypography.label,
                color = colors.textMuted
            )

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            Text(
                text = "$formattedTimeSaved saved",
                style = FocusTypography.display,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            Text(
                text = "$totalBlocked distraction attempts prevented on-device (~5m focus preserved each)",
                style = FocusTypography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun MetricsRow(
    todayCount: Int,
    weekCount: Int,
    allTimeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
    ) {
        MetricTile(
            label = "Today",
            count = todayCount,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            label = "Past 7 days",
            count = weekCount,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            label = "All time",
            count = allTimeCount,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = FocusShapes.medium,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = FocusTypography.headline,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = FocusTypography.caption,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun TopTargetItemCard(
    item: TargetStatItem,
    maxCount: Int
) {
    val colors = FocusTheme.colors
    val progress = (item.count.toFloat() / maxCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isApp = item.targetType == "APP"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = FocusShapes.medium,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isApp) {
                    FocusAppIcon(packageName = item.identifier, modifier = Modifier.size(32.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(FocusShapes.small)
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(FocusSpacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.displayName != item.identifier) {
                        Text(
                            text = item.identifier,
                            style = FocusTypography.caption,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = FocusShapes.small,
                    color = colors.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${item.count} ${if (item.count == 1) "time" else "times"}",
                        style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.sm))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(FocusShapes.small),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )
        }
    }
}

@Composable
private fun RecentEventItemCard(event: BlockEventEntity) {
    val colors = FocusTheme.colors
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(event.timestamp) { dateFormat.format(Date(event.timestamp)) }
    val isApp = event.targetType == "APP"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = FocusShapes.medium,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isApp) {
                FocusAppIcon(packageName = event.identifier, modifier = Modifier.size(28.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(FocusShapes.small)
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(FocusSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.identifier,
                    style = FocusTypography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.profileName} · $formattedTime",
                    style = FocusTypography.caption,
                    color = colors.textMuted
                )
            }
        }
    }
}
