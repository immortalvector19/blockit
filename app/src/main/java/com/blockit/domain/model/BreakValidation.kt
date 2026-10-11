package com.blockit.domain.model

/**
 * Encapsulates validation information and warnings when a user requests a pause/break.
 */
data class BreakWarningInfo(
    val exceedsRemainingSession: Boolean,
    val remainingSessionMs: Long,
    val requestedBreakMs: Long,
    val cappedBreakMinutes: Int,
    val newSessionEndMs: Long,
    val exceedsScheduleBoundary: Boolean = false,
    val scheduleEndTimestampMs: Long? = null
) {
    val excessDurationMs: Long = (requestedBreakMs - remainingSessionMs).coerceAtLeast(0L)
    val excessMinutes: Int = ((excessDurationMs + 59_999L) / 60_000L).toInt()
}

object BreakValidator {

    /**
     * Evaluates a requested break duration against the remaining focus session and optional schedule boundary.
     *
     * @param requestedMinutes The requested break length in minutes (e.g. 5, 10, 20)
     * @param remainingSessionMs Milliseconds remaining in the active session
     * @param sessionEndMs Target epoch timestamp when the session ends (0L if untimed)
     * @param scheduleEndMs Optional timestamp when the scheduled profile window ends
     * @param now Current epoch timestamp in ms (defaults to System.currentTimeMillis())
     */
    fun evaluate(
        requestedMinutes: Int,
        remainingSessionMs: Long,
        sessionEndMs: Long = 0L,
        scheduleEndMs: Long? = null,
        now: Long = System.currentTimeMillis()
    ): BreakWarningInfo {
        val requestedMs = requestedMinutes * 60 * 1000L
        val effectiveRemaining = remainingSessionMs.coerceAtLeast(0L)

        val exceedsSession = sessionEndMs > 0L && requestedMs > effectiveRemaining
        val cappedMinutes = ((effectiveRemaining + 59_999L) / 60_000L).toInt().coerceAtLeast(1)

        val newSessionEnd = if (sessionEndMs > 0L) {
            sessionEndMs + requestedMs
        } else {
            now + requestedMs
        }

        val exceedsSchedule = scheduleEndMs != null && scheduleEndMs > 0L && newSessionEnd > scheduleEndMs

        return BreakWarningInfo(
            exceedsRemainingSession = exceedsSession,
            remainingSessionMs = effectiveRemaining,
            requestedBreakMs = requestedMs,
            cappedBreakMinutes = cappedMinutes,
            newSessionEndMs = newSessionEnd,
            exceedsScheduleBoundary = exceedsSchedule,
            scheduleEndTimestampMs = scheduleEndMs
        )
    }
}
