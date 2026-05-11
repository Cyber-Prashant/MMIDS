package com.mmids.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MonitoringService : Service() {

    companion object {
        var isMonitoring = false
        const val CHANNEL_ID       = "mmids_service"
        const val ALERT_CHANNEL_ID = "mmids_alerts"
        const val NOTIF_ID         = 1001
        const val TAG              = "MMIDS"
    }

    private var appTracker: AppUsageTracker? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Standby"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START"    -> if (!isMonitoring) { isMonitoring = true;  startSession() }
            "STOP"     -> if (isMonitoring)  { isMonitoring = false; stopSession("MANUAL") }
            "SHUTDOWN" -> if (isMonitoring)  { isMonitoring = false; stopSession("DEVICE_SHUTDOWN") }
        }
        return START_STICKY
    }

    private fun startSession() {
        updateNotification("Active 🟢")
        writeLog("═══════════════════════════════════════")
        writeLog("SESSION START: ${ts()}")
        writeLog("═══════════════════════════════════════")
        appTracker = AppUsageTracker(this) { app ->
            writeLog("  [APP OPENED] $app @ ${ts()}")
        }.also { it.start() }
        Log.d(TAG, "🟢 Session started")
    }

    private fun stopSession(reason: String) {
        updateNotification("Standby")
        appTracker?.stop(); appTracker = null
        writeLog("───────────────────────────────────────")
        writeLog("SESSION END: ${ts()} | Reason: $reason")
        writeLog("═══════════════════════════════════════\n")
        showLogNotification()
        Log.d(TAG, "🔴 Session ended — $reason")
    }

    // ── Log I/O ──────────────────────────────────────────────────
    fun writeLog(text: String) {
        try {
            logFile().appendText("$text\n")
        } catch (e: Exception) { Log.e(TAG, "Log write: ${e.message}") }
    }

    private fun logFile(): File {
        val dir = File(filesDir, ".mmids_logs").also {
            if (!it.exists()) it.mkdirs()
            File(it, ".nomedia").also { n -> if (!n.exists()) n.createNewFile() }
        }
        return File(dir, "session_log.txt")
    }

    private fun ts() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    // ── Notifications ─────────────────────────────────────────────
    private fun buildNotification(content: String): Notification {
        val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
        val appName = prefs.getString("disguise_app_name", "MMID") ?: "System Service"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(appName)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification(content))
    }

    private fun showLogNotification() {
        val n = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MMIDS")
            .setContentText("📋 New Activity Report Available")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1002, n)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
