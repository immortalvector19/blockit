package com.blockit.ui.screens.schedules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.Schedule
import com.blockit.service.receiver.ScheduleManager
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusEmptyState
import com.blockit.ui.components.FocusTimePickerDialog
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.components.formatMinuteOfDayAmPm
import com.blockit.ui.components.fromMinuteOfDay
import com.blockit.ui.components.isOvernightWindow
import com.blockit.ui.components.toMinuteOfDay
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import kotlinx.coroutines.launch

private val DAYS_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorScreen(
    profileId: Long,
    profileRepo: ProfileRepository,
    ruleEvaluator: RuleEvaluator,
    scheduleManager: ScheduleManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    val schedules by profileRepo.observeSchedulesForProfile(profileId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showNewScheduleForm by rememberSaveable { mutableStateOf(false) }
    var editingScheduleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDaysMask by rememberSaveable { mutableIntStateOf(31) } // Mon-Fri default (bits 0..4)
    var startMinute by rememberSaveable { mutableIntStateOf(540) }     // 9:00 AM
    var endMinute by rememberSaveable { mutableIntStateOf(1020) }      // 5:00 PM
    var timePickerTarget by rememberSaveable { mutableStateOf<String?>(null) } // "start" or "end"

    Scaffold(
        modifier = modifier,
        topBar = {
            FocusTopBar(
                title = "Profile schedules",
                onBackClick = onBackClick
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FocusSpacing.lg)
        ) {
            FocusButton(
                text = if (showNewScheduleForm) "Hide schedule form" else "Add recurring schedule",
                onClick = {
                    if (!showNewScheduleForm) {
                        editingScheduleId = null
                        selectedDaysMask = 31
                        startMinute = 540
                        endMinute = 1020
                    }
                    showNewScheduleForm = !showNewScheduleForm
                },
                leadingIcon = if (showNewScheduleForm) Icons.Default.Close else Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(FocusSpacing.md))

            if (showNewScheduleForm) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FocusShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(FocusSpacing.md)) {
                        Text(
                            text = "Active days",
                            style = FocusTypography.label,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.sm))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DAYS_LABELS.forEachIndexed { index, label ->
                                val dayBit = 1 shl index
                                val isSelected = (selectedDaysMask and dayBit) != 0
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDaysMask = if (isSelected) {
                                            selectedDaysMask and dayBit.inv()
                                        } else {
                                            selectedDaysMask or dayBit
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = FocusTypography.caption
                                        )
                                    },
                                    shape = FocusShapes.small,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = colors.surface,
                                        labelColor = colors.textPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = colors.border,
                                        selectedBorderColor = colors.primary,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FocusSpacing.md))

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

                        // Plain Schedule Message under time fields
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

                        Spacer(modifier = Modifier.height(FocusSpacing.md))

                        FocusButton(
                            text = if (editingScheduleId != null) "Update schedule" else "Save schedule",
                            onClick = {
                                scope.launch {
                                    profileRepo.saveSchedule(
                                        Schedule(
                                            id = editingScheduleId ?: 0L,
                                            profileId = profileId,
                                            daysOfWeekMask = selectedDaysMask,
                                            startMinuteOfDay = startMinute,
                                            endMinuteOfDay = endMinute,
                                            isEnabled = true
                                        )
                                    )
                                    ruleEvaluator.refreshRules()
                                    scheduleManager.scheduleNextBoundaryAlarm()
                                    editingScheduleId = null
                                    showNewScheduleForm = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FocusSpacing.md))
            }

            if (schedules.isEmpty()) {
                FocusEmptyState(
                    title = "No recurring schedules",
                    message = "When no schedules are defined, this profile is only active when started manually."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm)) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onClick = {
                                editingScheduleId = schedule.id
                                selectedDaysMask = schedule.daysOfWeekMask
                                startMinute = schedule.startMinuteOfDay
                                endMinute = schedule.endMinuteOfDay
                                showNewScheduleForm = true
                            },
                            onToggle = { isEnabled ->
                                scope.launch {
                                    profileRepo.saveSchedule(schedule.copy(isEnabled = isEnabled))
                                    ruleEvaluator.refreshRules()
                                    scheduleManager.scheduleNextBoundaryAlarm()
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    profileRepo.deleteSchedule(schedule.id)
                                    ruleEvaluator.refreshRules()
                                    scheduleManager.scheduleNextBoundaryAlarm()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (timePickerTarget != null) {
        val isStart = timePickerTarget == "start"
        FocusTimePickerDialog(
            initialMinuteOfDay = if (isStart) startMinute else endMinute,
            title = if (isStart) "Select start time" else "Select end time",
            onDismiss = { timePickerTarget = null },
            onTimeSelected = { selectedMin ->
                if (isStart) {
                    startMinute = selectedMin
                } else {
                    endMinute = selectedMin
                }
                timePickerTarget = null
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val colors = FocusTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = FocusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val timeRange = "${formatMinuteOfDayAmPm(schedule.startMinuteOfDay)} to ${formatMinuteOfDayAmPm(schedule.endMinuteOfDay)}"
                val isOvernight = isOvernightWindow(schedule.startMinuteOfDay, schedule.endMinuteOfDay)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeRange,
                        style = FocusTypography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    if (isOvernight) {
                        Spacer(modifier = Modifier.width(FocusSpacing.xs))
                        Surface(
                            shape = FocusShapes.small,
                            color = colors.statusWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Overnight",
                                style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                color = colors.statusWarning,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val days = buildList {
                    DAYS_LABELS.forEachIndexed { idx, label ->
                        if ((schedule.daysOfWeekMask and (1 shl idx)) != 0) add(label)
                    }
                }.joinToString(", ")
                Text(
                    text = days.ifEmpty { "No days selected" },
                    style = FocusTypography.caption,
                    color = colors.textMuted
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = colors.statusDestructive
                )
            }

            Spacer(modifier = Modifier.width(FocusSpacing.xs))

            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surfaceVariant
                )
            )
        }
    }
}
