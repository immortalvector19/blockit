package com.blockit.service.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class BlockItDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Deactivating BlockIt Device Admin removes uninstall protection for active focus sessions."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
