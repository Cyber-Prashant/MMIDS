package com.mmids.receivers

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.mmids.services.MonitoringService
import com.mmids.ui.screens.IntruderAlertActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Volume Button Receiver ───────────────────────────────────────
class VolumeReceiver : BroadcastReceiver() {
    companion object { private var lastVolume = -1 }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.media.VOLUME_CHANGED_ACTION") return
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) return // Only act when screen is LOCKED

        // Check if lock trigger is enabled in settings
        val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("lock_trigger", true)) return

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val vol = am.getStreamVolume(AudioManager.STREAM_RING)

        if (lastVolume == -1) { lastVolume = vol; return }

        val svcIntent = Intent(context, MonitoringService::class.java)
        when {
            (vol > lastVolume) && !MonitoringService.isMonitoring -> {
                svcIntent.action = "START"
                context.startForegroundService(svcIntent)
                Log.d("MMIDS", "🟢 Monitoring ON via Vol UP")
            }
            vol < lastVolume && MonitoringService.isMonitoring -> {
                svcIntent.action = "STOP"
                context.startForegroundService(svcIntent)
                Log.d("MMIDS", "🔴 Monitoring OFF via Vol DOWN")
            }
        }
        lastVolume = vol
    }
}

// ── Screen Unlock Receiver ───────────────────────────────────────
class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        val mode = if (MonitoringService.isMonitoring) "DETERRENCE" else "PROMPT"
        context.startActivity(
            Intent(context, IntruderAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("mode", mode)
            }
        )
    }
}

// ── Dialer Code Receiver ─────────────────────────────────────────
class DialerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = resultData ?: return
        if (number.trim() == "*#66437#") {
            resultData = null
            context.startActivity(
                Intent(context, com.mmids.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }
}

// ── Shutdown Receiver ────────────────────────────────────────────
class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!MonitoringService.isMonitoring) return
        MonitoringService.isMonitoring = false
        try {
            context.startForegroundService(
                Intent(context, MonitoringService::class.java).apply { action = "SHUTDOWN" }
            )
        } catch (_: Exception) {
            // Fallback: write directly
            try {
                val dir = File(context.filesDir, ".mmids_logs").also { it.mkdirs() }
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                File(dir, "session_log.txt").appendText(
                    "───────────────────────────────────────\n" +
                    "SESSION END: $ts | Reason: DEVICE_SHUTDOWN\n" +
                    "═══════════════════════════════════════\n\n"
                )
            } catch (_: Exception) {}
        }
    }
}

// ── Boot Receiver ────────────────────────────────────────────────
// Auto-starts the monitoring service when the phone finishes booting,
// so the app resumes silently after every restart.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bootAction = intent.action ?: return
        if (bootAction != Intent.ACTION_BOOT_COMPLETED &&
            bootAction != "android.intent.action.QUICKBOOT_POWERON" &&
            bootAction != "com.htc.intent.action.QUICKBOOT_POWERON") return

        val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("start_on_boot", true)) return

        try {
            context.startForegroundService(
                Intent(context, MonitoringService::class.java).apply { action = "START" }
            )
            Log.d("MMIDS", "🚀 Auto-started after boot")
        } catch (e: Exception) {
            Log.e("MMIDS", "Boot auto-start failed: ${e.message}")
        }
    }
}
