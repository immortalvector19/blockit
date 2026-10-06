package com.blockit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.ui.theme.BlockitTheme
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Converts 12-hour AM/PM representation into 0..1439 minute-of-day.
 */
fun toMinuteOfDay(hour12: Int, minute: Int, isPm: Boolean): Int {
    val h24 = when {
        isPm && hour12 < 12 -> hour12 + 12
        !isPm && hour12 == 12 -> 0
        else -> hour12
    }
    return ((h24 * 60) + minute) % 1440
}

/**
 * Converts 0..1439 minute-of-day into Triple(hour12, minute, isPm).
 */
fun fromMinuteOfDay(minuteOfDay: Int): Triple<Int, Int, Boolean> {
    val sanitized = (minuteOfDay % 1440 + 1440) % 1440
    val h24 = sanitized / 60
    val m = sanitized % 60
    val isPm = h24 >= 12
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return Triple(h12, m, isPm)
}

/**
 * Formats a 0..1439 minute-of-day into standard 12-hour AM/PM format (e.g. "9:00 AM", "5:00 PM").
 */
fun formatMinuteOfDayAmPm(minuteOfDay: Int): String {
    val (h12, m, isPm) = fromMinuteOfDay(minuteOfDay)
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, if (isPm) "PM" else "AM")
}

/**
 * Returns true if the window spans overnight (start time is strictly greater than end time).
 */
fun isOvernightWindow(startMinute: Int, endMinute: Int): Boolean {
    return startMinute > endMinute
}

/**
 * A sleek, touch-scrollable and clickable wheel picker for numeric/text options.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 40.dp
) {
    val colors = BlockitTheme.colors
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != selectedIndex) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, items.size - 1))
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > 30) 1 else 0
            val target = centerIndex.coerceIn(0, items.size - 1)
            if (target != selectedIndex) {
                onSelectedIndexChanged(target)
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Center selection bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            shape = RoundedCornerShape(8.dp),
            color = colors.primary.copy(alpha = 0.12f)
        ) {}

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemsCount - 1) / 2))
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            onSelectedIndexChanged(index)
                            scope.launch { listState.animateScrollToItem(index) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = if (isSelected) MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp
                        ) else MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textMuted.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Three-column scroll wheel picker for 12-hour AM/PM time selection.
 */
@Composable
fun AmPmTimeWheelPicker(
    hour12: Int, // 1..12
    minute: Int, // 0..59
    isPm: Boolean,
    onTimeChanged: (hour12: Int, minute: Int, isPm: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BlockitTheme.colors
    val hours = (1..12).map { it.toString() }
    val minutes = (0..59).map { String.format(Locale.getDefault(), "%02d", it) }
    val periods = listOf("AM", "PM")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour Wheel
        WheelPicker(
            items = hours,
            selectedIndex = (hour12 - 1).coerceIn(0, 11),
            onSelectedIndexChanged = { idx ->
                onTimeChanged(idx + 1, minute, isPm)
            },
            modifier = Modifier.width(64.dp)
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            ),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Minute Wheel
        WheelPicker(
            items = minutes,
            selectedIndex = minute.coerceIn(0, 59),
            onSelectedIndexChanged = { idx ->
                onTimeChanged(hour12, idx, isPm)
            },
            modifier = Modifier.width(64.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // AM/PM Wheel
        WheelPicker(
            items = periods,
            selectedIndex = if (isPm) 1 else 0,
            onSelectedIndexChanged = { idx ->
                onTimeChanged(hour12, minute, idx == 1)
            },
            modifier = Modifier.width(64.dp)
        )
    }
}
