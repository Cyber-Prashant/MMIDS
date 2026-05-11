package com.mmids.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class MMIDSDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        android.util.Log.d("MMIDS", "Device Admin enabled")
    }
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "System service cannot be disabled. Contact device administrator."
}
