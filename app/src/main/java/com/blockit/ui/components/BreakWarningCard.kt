package com.blockit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.domain.model.BreakWarningInfo
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import com.blockit.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-craft Oil UI / Apple HIG warning card displayed when a requested pause
 * exceeds the remaining focus session or schedule boundary.
 */
@Composable
fun BreakWarningCard(
    warningInfo: BreakWarningInfo,
    selectedMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (!warningInfo.exceedsRemainingSession) return

    val colors = FocusTheme.colors
    val formattedNewEnd = remember(warningInfo.newSessionEndMs) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(warningInfo.newSessionEndMs))
    }
    val remMinutes = ((warningInfo.remainingSessionMs + 59_999L) / 60_000L).toInt()
    val remSeconds = (warningInfo.remainingSessionMs / 1000L) % 60L
    val remainingFormatted = if (remMinutes > 0) "${remMinutes}m" else "${remSeconds}s"

    Surface(
        shape = FocusShapes.small,
        color = WarningAmber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FocusSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Break exceeds remaining session",
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = WarningAmber
                )
            }

            Text(
                text = "This ${selectedMinutes}m break exceeds your remaining focus time ($remainingFormatted). Starting it will extend your focus session by ${selectedMinutes}m until $formattedNewEnd.${if (warningInfo.exceedsScheduleBoundary) " (Extends past schedule boundary)" else ""}",
                style = FocusTypography.caption,
                color = colors.textPrimary
            )
        }
    }
}
