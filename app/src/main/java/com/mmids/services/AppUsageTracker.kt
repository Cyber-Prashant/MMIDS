package com.mmids.services

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.*

class AppUsageTracker(
    private val context: Context,
    private val onAppOpened: (String) -> Unit,
) {
    private var job: Job? = null
    private var lastApp = ""

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val current = getForeground()
                if (current != null && (current != lastApp) && (current != context.packageName)) {
                    lastApp = current
                    val name = getLabel(current)
                    withContext(Dispatchers.Main) { onAppOpened(name) }
                }
                delay(1500)
            }
        }
    }

    fun stop() { job?.cancel(); job = null; lastApp = "" }

    private fun getForeground(): String? = try {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10000, now)
            ?.maxByOrNull { it.lastTimeUsed }?.packageName
    } catch (_: Exception) { null }

    private fun getLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) { pkg }
}
