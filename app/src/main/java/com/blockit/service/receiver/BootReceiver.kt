package com.blockit.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blockit.core.BlockItApplication
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val container = BlockItApplication.from(context)
            container.applicationScope.launch {
                // Reconcile focus sessions: purge expired sessions, re-arm alarms, resume active sessions
                val now = System.currentTimeMillis()
                val reconcileResult = container.focusSessionManager.reconcileSessionOnBoot(now)

                val snapshot = container.ruleEvaluator.getSnapshot()
                if (snapshot.hasActiveWhitelist && reconcileResult.hasActiveSession) {
                    val homeIntent = Intent(context, com.blockit.ui.screens.focushome.FocusHomeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(homeIntent)
                }
            }
        }
    }
}
