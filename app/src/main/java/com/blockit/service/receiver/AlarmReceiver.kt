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

            // 3. Enforce Focus Home lockdown if Allowlist schedule triggered
            val snapshot = container.ruleEvaluator.getSnapshot()
            if (snapshot.hasActiveWhitelist && !snapshot.isWhitelistExpired(now)) {
                try {
                    val homeIntent = Intent(context, com.blockit.ui.screens.focushome.FocusHomeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(homeIntent)
                } catch (_: Exception) {}
            }

            // 4. Re-arm next boundary alarm
            container.scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    companion object {
        const val ACTION_SCHEDULE_BOUNDARY = "com.blockit.action.SCHEDULE_BOUNDARY"
    }
}
