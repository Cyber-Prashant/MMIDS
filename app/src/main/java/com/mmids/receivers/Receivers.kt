package com.mmids.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mmids.services.MonitoringService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Screen Unlock Receiver ───────────────────────────────────────
class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        Log.d("MMIDS", "🔓 Unlock detected")
        
        val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
        val isMonitoring = prefs.getBoolean("is_monitoring_active", false)
        val mode = if (isMonitoring) "DETERRENCE" else "PROMPT"
        
        // Trigger the service to launch the intruder activity using high priority notification/FullScreenIntent
        val svcIntent = Intent(context, MonitoringService::class.java).apply {
            action = "LAUNCH_INTRUDER"
            putExtra("mode", mode)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent)
            } else {
                context.startService(svcIntent)
            }
        } catch (e: Exception) {
            Log.e("MMIDS", "Failed to notify service for intruder: ${e.message}")
        }
    }
}

// ── Dialer Code Receiver ─────────────────────────────────────────
class DialerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = resultData ?: intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
        val secretCode = intent.data?.host ?: ""
        Log.d("MMIDS", "DialerReceiver triggered: number=$number, secretCode=$secretCode")
        
        if (number.trim() == "*#66437#" || secretCode == "66437") {
            resultData = null
            val mainIntent = Intent(context, com.mmids.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(mainIntent)
        }
    }
}

// ── Shutdown Receiver ────────────────────────────────────────────
class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_monitoring_active", false)) return
        
        try {
            context.startForegroundService(
                Intent(context, MonitoringService::class.java).apply { action = "SHUTDOWN" }
            )
        } catch (_: Exception) {
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
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bootAction = intent.action ?: return
        if (bootAction != Intent.ACTION_BOOT_COMPLETED &&
            bootAction != "android.intent.action.QUICKBOOT_POWERON" &&
            bootAction != "com.htc.intent.action.QUICKBOOT_POWERON") return

        val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("user_consented", false)) return
        if (!prefs.getBoolean("auto_start_boot", true)) return

        try {
            val svcIntent = Intent(context, MonitoringService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent)
            } else {
                context.startService(svcIntent)
            }
            Log.d("MMIDS", "🚀 Auto-started after boot")
        } catch (e: Exception) {
            Log.e("MMIDS", "Boot auto-start failed: ${e.message}")
        }
    }
}
