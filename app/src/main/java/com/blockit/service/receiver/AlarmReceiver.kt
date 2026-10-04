package com.blockit.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blockit.core.BlockItApplication
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = BlockItApplication.from(context)
        container.applicationScope.launch {
            val now = System.currentTimeMillis()
            // 1. Cleanup expired sessions
            container.database.focusSessionDao().cleanupExpiredSessions(now)

            // 2. Refresh active rules in engine
            container.ruleEvaluator.refreshRules()

            // 3. Re-arm next boundary alarm
            container.scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    companion object {
        const val ACTION_SCHEDULE_BOUNDARY = "com.blockit.action.SCHEDULE_BOUNDARY"
    }
}
