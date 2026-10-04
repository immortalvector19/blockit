package com.blockit.ui.screens.schedules

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.Schedule
import com.blockit.service.receiver.ScheduleManager
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
    val schedules by profileRepo.observeSchedulesForProfile(profileId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showNewScheduleForm by remember { mutableStateOf(false) }
    var selectedDaysMask by remember { mutableIntStateOf(31) } // Mon-Fri default
    var startHourText by remember { mutableStateOf("09") }
    var startMinText by remember { mutableStateOf("00") }
    var endHourText by remember { mutableStateOf("17") }
    var endMinText by remember { mutableStateOf("00") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile Schedules") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(
                onClick = { showNewScheduleForm = !showNewScheduleForm },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showNewScheduleForm) "Hide Schedule Form" else "Add Recurring Schedule Window")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showNewScheduleForm) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Days of Week:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))

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
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Time (24h)", style = MaterialTheme.typography.labelMedium)
                                Row {
                                    OutlinedTextField(
                                        value = startHourText,
                                        onValueChange = { if (it.length <= 2) startHourText = it },
                                        label = { Text("HH") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(
                                        value = startMinText,
                                        onValueChange = { if (it.length <= 2) startMinText = it },
                                        label = { Text("MM") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("End Time (24h)", style = MaterialTheme.typography.labelMedium)
                                Row {
                                    OutlinedTextField(
                                        value = endHourText,
                                        onValueChange = { if (it.length <= 2) endHourText = it },
                                        label = { Text("HH") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(
                                        value = endMinText,
                                        onValueChange = { if (it.length <= 2) endMinText = it },
                                        label = { Text("MM") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val sH = startHourText.toIntOrNull() ?: 9
                                val sM = startMinText.toIntOrNull() ?: 0
                                val eH = endHourText.toIntOrNull() ?: 17
                                val eM = endMinText.toIntOrNull() ?: 0

                                val startTotal = (sH.coerceIn(0, 23) * 60) + sM.coerceIn(0, 59)
                                val endTotal = (eH.coerceIn(0, 23) * 60) + eM.coerceIn(0, 59)

                                scope.launch {
                                    profileRepo.saveSchedule(
                                        Schedule(
                                            profileId = profileId,
                                            daysOfWeekMask = selectedDaysMask,
                                            startMinuteOfDay = startTotal,
                                            endMinuteOfDay = endTotal,
                                            isEnabled = true
                                        )
                                    )
                                    ruleEvaluator.refreshRules()
                                    scheduleManager.scheduleNextBoundaryAlarm()
                                    showNewScheduleForm = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Schedule")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (schedules.isEmpty()) {
                Text(
                    "No recurring schedules. When no schedules are defined, this profile is active whenever toggled ON.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
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
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val sHour = schedule.startMinuteOfDay / 60
                val sMin = schedule.startMinuteOfDay % 60
                val eHour = schedule.endMinuteOfDay / 60
                val eMin = schedule.endMinuteOfDay % 60

                Text(
                    text = String.format("%02d:%02d – %02d:%02d", sHour, sMin, eHour, eMin),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val days = buildList {
                    DAYS_LABELS.forEachIndexed { idx, label ->
                        if ((schedule.daysOfWeekMask and (1 shl idx)) != 0) add(label)
                    }
                }.joinToString(", ")
                Text(
                    text = days.ifEmpty { "No days" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
