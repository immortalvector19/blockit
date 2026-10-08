package com.blockit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockit.domain.model.ListType
import com.blockit.domain.model.ProfileType
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

/**
 * Full-screen New Profile creation flow.
 * Replaces the tall dialog with an edge-to-edge page that handles keyboard input seamlessly.
 */
@Composable
fun CreateProfileDialog(
    initialType: ProfileType = ProfileType.FOCUS,
    initialStartMinute: Int = 540,
    initialEndMinute: Int = 1020,
    initialDaysMask: Int = 127,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        profileType: ProfileType,
        listType: ListType,
        daysMask: Int,
        startMinute: Int,
        endMinute: Int
    ) -> Unit
) {
    val colors = FocusTheme.colors

    var name by rememberSaveable { mutableStateOf("") }
    var selectedProfileType by rememberSaveable { mutableStateOf(initialType) }
    var selectedListType by rememberSaveable { mutableStateOf(ListType.BLOCKLIST) }

    var daysMask by rememberSaveable { mutableIntStateOf(initialDaysMask) }
    var startMinute by rememberSaveable { mutableIntStateOf(initialStartMinute) }
    var endMinute by rememberSaveable { mutableIntStateOf(initialEndMinute) }
    var timePickerTarget by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        color = colors.background
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Text(
                        text = "New profile",
                        style = FocusTypography.headline,
                        color = colors.textPrimary
                    )
                }

                HorizontalDivider(color = colors.border, thickness = 1.dp)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(FocusSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.xl)
                ) {
                    // SECTION 1: Profile Name
                    Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                        Text(
                            text = "Profile name",
                            style = FocusTypography.label,
                            color = colors.textSecondary
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g. Deep Work, Evening Offline", color = colors.textMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = FocusShapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )
                    }

                    // SECTION 2: Profile Type (Focus vs Schedule)
                    Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                        Text(
                            text = "Type",
                            style = FocusTypography.label,
                            color = colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                        ) {
                            ProfileChoiceCard(
                                title = "Focus",
                                subtitle = "Start it manually from Home.",
                                icon = Icons.Rounded.PlayArrow,
                                isSelected = selectedProfileType == ProfileType.FOCUS,
                                onClick = { selectedProfileType = ProfileType.FOCUS },
                                modifier = Modifier.weight(1f)
                            )
                            ProfileChoiceCard(
                                title = "Schedule",
                                subtitle = "Runs automatically on days & times.",
                                icon = Icons.Rounded.Schedule,
                                isSelected = selectedProfileType == ProfileType.SCHEDULE,
                                onClick = { selectedProfileType = ProfileType.SCHEDULE },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // SECTION 3: List Type (Blocklist vs Allowlist)
                    Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                        Text(
                            text = "List",
                            style = FocusTypography.label,
                            color = colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                        ) {
                            ProfileChoiceCard(
                                title = "Blocklist",
                                subtitle = "Blocks chosen apps & sites.",
                                icon = Icons.Rounded.FilterList,
                                isSelected = selectedListType == ListType.BLOCKLIST,
                                onClick = { selectedListType = ListType.BLOCKLIST },
                                modifier = Modifier.weight(1f)
                            )
                            ProfileChoiceCard(
                                title = "Allowlist",
                                subtitle = "Locks down to permitted apps only.",
                                icon = Icons.Rounded.CalendarMonth,
                                isSelected = selectedListType == ListType.ALLOWLIST,
                                onClick = { selectedListType = ListType.ALLOWLIST },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // SECTION 4: Schedule Settings (Only visible if Type == Schedule)
                    if (selectedProfileType == ProfileType.SCHEDULE) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = FocusShapes.medium,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(FocusSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
                            ) {
                                Text(
                                    text = "Days active",
                                    style = FocusTypography.label,
                                    color = colors.textSecondary
                                )

                                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    dayLabels.forEachIndexed { i, label ->
                                        val bit = 1 shl i
                                        val isSelected = (daysMask and bit) != 0
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                daysMask = if (isSelected) daysMask and bit.inv() else daysMask or bit
                                            },
                                            label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.primary,
                                                selectedLabelColor = Color.White,
                                                containerColor = colors.surfaceVariant,
                                                labelColor = colors.textSecondary
                                            ),
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(FocusSpacing.sm))

                                // Two Tappable Time Fields
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { timePickerTarget = "start" },
                                        shape = FocusShapes.small,
                                        color = colors.surfaceVariant,
                                        border = BorderStroke(1.dp, colors.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = FocusSpacing.md),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Start",
                                                    style = FocusTypography.caption,
                                                    color = colors.textSecondary
                                                )
                                                Text(
                                                    text = formatMinuteOfDayAmPm(startMinute),
                                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.textPrimary
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.Schedule,
                                                contentDescription = "Change start time",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { timePickerTarget = "end" },
                                        shape = FocusShapes.small,
                                        color = colors.surfaceVariant,
                                        border = BorderStroke(1.dp, colors.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = FocusSpacing.md),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "End",
                                                    style = FocusTypography.caption,
                                                    color = colors.textSecondary
                                                )
                                                Text(
                                                    text = formatMinuteOfDayAmPm(endMinute),
                                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.textPrimary
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.Schedule,
                                                contentDescription = "Change end time",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(FocusSpacing.xs))

                                // Plain Schedule Message
                                if (startMinute == endMinute) {
                                    Text(
                                        text = "Start and end can't be the same",
                                        style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                        color = colors.statusWarning
                                    )
                                } else if (startMinute > endMinute) {
                                    Text(
                                        text = "Runs ${formatMinuteOfDayAmPm(startMinute)} to ${formatMinuteOfDayAmPm(endMinute)} (next day)",
                                        style = FocusTypography.caption,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Fixed Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FocusSpacing.lg)
                    ) {
                        FocusButton(
                            text = "Create profile",
                            onClick = {
                                if (name.isNotBlank()) {
                                    onConfirm(
                                        name.trim(),
                                        selectedProfileType,
                                        selectedListType,
                                        daysMask,
                                        startMinute,
                                        endMinute
                                    )
                                }
                            },
                            enabled = name.isNotBlank() && (selectedProfileType != ProfileType.SCHEDULE || (daysMask != 0 && startMinute != endMinute)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

    if (timePickerTarget != null) {
        val isTargetStart = timePickerTarget == "start"
        val currentMinute = if (isTargetStart) startMinute else endMinute
        FocusTimePickerDialog(
            title = if (isTargetStart) "Select start time" else "Select end time",
            initialMinuteOfDay = currentMinute,
            onDismiss = { timePickerTarget = null },
            onTimeSelected = { chosenMinuteOfDay ->
                if (isTargetStart) {
                    startMinute = chosenMinuteOfDay
                } else {
                    endMinute = chosenMinuteOfDay
                }
                timePickerTarget = null
            }
        )
    }
}

@Composable
private fun ProfileChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors

    Surface(
        shape = FocusShapes.medium,
        color = if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.border
        ),
        modifier = modifier
            .clip(FocusShapes.medium)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) colors.primary else colors.textPrimary
                )
            }
            Text(
                text = subtitle,
                style = FocusTypography.caption,
                color = colors.textSecondary,
                minLines = 2
            )
        }
    }
}

/**
 * Modern Apple-style dialog for editing an existing schedule profile's active days
 * and start/end time windows.
 */
@Composable
fun EditScheduleDialog(
    profileName: String,
    initialDaysMask: Int,
    initialStartMinute: Int,
    initialEndMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (daysMask: Int, startMinute: Int, endMinute: Int) -> Unit
) {
    val colors = FocusTheme.colors
    var daysMask by rememberSaveable { mutableIntStateOf(initialDaysMask) }
    var startMinute by rememberSaveable { mutableIntStateOf(initialStartMinute) }
    var endMinute by rememberSaveable { mutableIntStateOf(initialEndMinute) }
    var timePickerTarget by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        color = colors.background
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        text = "Edit schedule",
                        style = FocusTypography.headline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                HorizontalDivider(color = colors.border)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
                ) {
                    // Profile Name Display
                    Surface(
                        shape = FocusShapes.medium,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(FocusSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = profileName,
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Scheduled automatic focus",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    // Active Days
                    Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                        Text(
                            text = "Active days",
                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayLabels.forEachIndexed { i, label ->
                                val bit = 1 shl i
                                val isSelected = (daysMask and bit) != 0
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        daysMask = if (isSelected) daysMask and bit.inv() else daysMask or bit
                                    },
                                    label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = colors.surfaceVariant,
                                        labelColor = colors.textSecondary
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    // Time Window
                    Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm)) {
                        Text(
                            text = "Time window",
                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.md)
                        ) {
                            Surface(
                                onClick = { timePickerTarget = "start" },
                                shape = FocusShapes.small,
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = FocusSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Start",
                                            style = FocusTypography.caption,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = formatMinuteOfDayAmPm(startMinute),
                                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.textPrimary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = "Change start time",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { timePickerTarget = "end" },
                                shape = FocusShapes.small,
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = FocusSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "End",
                                            style = FocusTypography.caption,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = formatMinuteOfDayAmPm(endMinute),
                                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.textPrimary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = "Change end time",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Plain Schedule Message
                        if (startMinute == endMinute) {
                            Text(
                                text = "Start and end can't be the same",
                                style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                color = colors.statusWarning
                            )
                        } else if (startMinute > endMinute) {
                            Text(
                                text = "Runs ${formatMinuteOfDayAmPm(startMinute)} to ${formatMinuteOfDayAmPm(endMinute)} (next day)",
                                style = FocusTypography.caption,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FocusSpacing.lg)
                    ) {
                        FocusButton(
                            text = "Save changes",
                            onClick = {
                                onConfirm(daysMask, startMinute, endMinute)
                            },
                            enabled = daysMask != 0 && startMinute != endMinute,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

    if (timePickerTarget != null) {
        val isTargetStart = timePickerTarget == "start"
        val currentMinute = if (isTargetStart) startMinute else endMinute
        FocusTimePickerDialog(
            title = if (isTargetStart) "Select start time" else "Select end time",
            initialMinuteOfDay = currentMinute,
            onDismiss = { timePickerTarget = null },
            onTimeSelected = { chosenMinuteOfDay ->
                if (isTargetStart) {
                    startMinute = chosenMinuteOfDay
                } else {
                    endMinute = chosenMinuteOfDay
                }
                timePickerTarget = null
            }
        )
    }
}
