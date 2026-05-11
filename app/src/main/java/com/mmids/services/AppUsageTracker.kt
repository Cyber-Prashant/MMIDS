package com.mmids.services

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.*

class AppUsageTracker(
    private val context: Context,
    private val onAppOpened: (String) -> Unit,
) {
    private var job: Job? = null
    private var lastApp = ""

    fun start() {
        if (!hasUsagePermission()) return
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val current = getForegroundApp()
                if (current != null && current != lastApp && current != context.packageName) {
                    if (!isIgnoredApp(current)) {
                        lastApp = current
                        val name = getLabel(current)
                        withContext(Dispatchers.Main) { onAppOpened(name) }
                    }
                }
                delay(1500)
            }
        }
    }

    fun stop() { job?.cancel(); job = null; lastApp = "" }

    private fun getForegroundApp(): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 15000, now)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun getLabel(pkg: String): String = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getApplicationInfo(pkg, 0)
        }
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) { pkg }

    private fun isIgnoredApp(pkg: String): Boolean {
        val ignored = listOf("com.android.systemui", "com.android.launcher", "com.google.android.apps.nexuslauncher", "com.mmids")
        return ignored.any { pkg.contains(it) }
    }

    fun hasUsagePermission(): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60000, now)
        return stats != null && stats.isNotEmpty()
    }
}
