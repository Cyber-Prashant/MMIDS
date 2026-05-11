package com.mmids.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mmids.receivers.UnlockReceiver
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
    private val unlockReceiver = UnlockReceiver()
    private var lastVolume = -1

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (!km.isKeyguardLocked) return

            val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("lock_trigger", true)) return

            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            // Check both RING and MUSIC streams as different devices use different ones for physical buttons
            val ringVol = am.getStreamVolume(AudioManager.STREAM_RING)
            val musicVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val vol = ringVol + musicVol

            if (lastVolume == -1) {
                lastVolume = vol
                return
            }

            if (vol > lastVolume && !isMonitoring) {
                isMonitoring = true
                prefs.edit().putBoolean("is_monitoring_active", true).apply()
                startSession()
                Log.d(TAG, "🟢 Monitoring ON via Vol UP")
            } else if (vol < lastVolume && isMonitoring) {
                isMonitoring = false
                prefs.edit().putBoolean("is_monitoring_active", false).apply()
                stopSession("MANUAL")
                Log.d(TAG, "🔴 Monitoring OFF via Vol DOWN")
            }
            lastVolume = vol
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Standby"))

        // Register ContentObserver for volume changes
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )

        // Register UnlockReceiver dynamically
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT).apply {
            priority = 1000
        }
        registerReceiver(unlockReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
        
        if (intent != null) {
            when (intent.action) {
                "START"    -> if (!isMonitoring) { 
                    isMonitoring = true
                    prefs.edit().putBoolean("is_monitoring_active", true).apply()
                    startSession() 
                }
                "STOP"     -> if (isMonitoring)  { 
                    isMonitoring = false
                    prefs.edit().putBoolean("is_monitoring_active", false).apply()
                    stopSession("MANUAL") 
                }
                "SHUTDOWN" -> if (isMonitoring)  { 
                    isMonitoring = false
                    prefs.edit().putBoolean("is_monitoring_active", false).apply()
                    stopSession("DEVICE_SHUTDOWN") 
                }
                "UPDATE_NOTIF" -> {
                    updateNotification(if (isMonitoring) "Active 🟢" else "Standby")
                }
            }
        } else {
            // Service restarted by system
            val persistedState = prefs.getBoolean("is_monitoring_active", false)
            if (persistedState && !isMonitoring) {
                isMonitoring = true
                startSession()
            }
        }
        return START_STICKY
    }

    private fun startSession() {
        updateNotification("Active 🟢")
        writeLog("═══════════════════════════════════════")
        writeLog("SESSION START: ${ts()}")
        writeLog("═══════════════════════════════════════")
        appTracker = AppUsageTracker(this) { app ->
            writeLog("  [APP OPENED] $app @ ${ts_time()}")
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

    fun writeLog(text: String) {
        try {
            val dir = File(filesDir, ".mmids_logs").also { if (!it.exists()) it.mkdirs() }
            File(dir, ".nomedia").also { if (!it.exists()) it.createNewFile() }
            File(dir, "session_log.txt").appendText("$text\n")
        } catch (e: Exception) { Log.e(TAG, "Log write error: ${e.message}") }
    }

    private fun ts() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    private fun ts_time() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private fun buildNotification(content: String): Notification {
        val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
        val appName = prefs.getString("disguise_app_name", "MMID") ?: "MMID"
        
        val mainIntent = Intent(this, com.mmids.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(appName)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(content))
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

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(volumeObserver)
        try { unregisterReceiver(unlockReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
