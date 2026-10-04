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
                // Re-evaluate and re-prime the rule cache following system restart or time shifts
                container.ruleEvaluator.refreshRules()
            }
        }
    }
}
