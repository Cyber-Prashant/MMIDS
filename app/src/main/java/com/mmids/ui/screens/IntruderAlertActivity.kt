package com.mmids.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmids.MainActivity
import com.mmids.admin.MMIDSDeviceAdmin
import com.mmids.services.MonitoringService
import com.mmids.ui.theme.*
import java.io.File

class IntruderAlertActivity : ComponentActivity() {

    private val LONG_PRESS_MS = 5000L
    private var holdHandler: Handler? = null
    private var holdRunnable: Runnable? = null
    private var authorized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MMIDS", "IntruderAlertActivity: onCreate")

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val mode = intent.getStringExtra("mode") ?: "PROMPT"
        if (mode == "DETERRENCE") playShutterSound()

        setContent {
            MMIDSTheme {
                BackHandler { /* Block back press on security screen */ }
                when (mode) {
                    "DETERRENCE" -> DeterrenceScreen(onFinish = { finish() })
                    else -> IntruderPromptScreen(
                        onOkPress = { startHold() },
                        onOkRelease = { elapsed -> endHold(elapsed) },
                        onCancel = { lockAndExit() }
                    )
                }
            }
        }
    }

    private fun startHold() {
        authorized = false
        holdHandler = Handler(Looper.getMainLooper())
        holdRunnable = Runnable {
            Log.d("MMIDS", "IntruderAlertActivity: User authorized via long press")
            authorized = true
            onAuthorized()
        }
        holdHandler?.postDelayed(holdRunnable!!, LONG_PRESS_MS)
    }

    private fun endHold(elapsed: Long) {
        val wasAuthorized = authorized
        holdHandler?.removeCallbacks(holdRunnable!!)
        if (!wasAuthorized && elapsed > 0) {
            Log.d("MMIDS", "IntruderAlertActivity: Short press detected - Intruder mode triggered")
            playShutterSound()
            onIntruder()
        }
    }

    private fun onIntruder() {
        val svcIntent = Intent(this, MonitoringService::class.java).apply { action = "START" }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }
        setContent {
            MMIDSTheme { DeterrenceScreen(onFinish = { finish() }) }
        }
    }

    private fun onAuthorized() {
        val svcIntent = Intent(this, MonitoringService::class.java).apply { action = "STOP" }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }
        setContent {
            MMIDSTheme {
                AuthorizedScreen(
                    onViewLogs = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onProceed = { finish() }
                )
            }
        }
    }

    private fun lockAndExit() {
        try {
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, MMIDSDeviceAdmin::class.java)
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow()
            }
        } catch (e: Exception) {
            Log.e("MMIDS", "IntruderAlertActivity: Lock failed: ${e.message}")
        }
        finish()
    }

    private fun playShutterSound() {
        try {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            val prefs = getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
            val path = prefs.getString("shutter_sound_path", "")
            val player = if (!path.isNullOrEmpty() && File(path).exists()) {
                MediaPlayer.create(this, android.net.Uri.fromFile(File(path)))
            } else {
                MediaPlayer.create(this, com.mmids.R.raw.shutter)
            }
            player?.start()
            player?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("MMIDS", "IntruderAlertActivity: Audio error: ${e.message}")
        }
    }
}

@Composable
fun IntruderPromptScreen(
    onOkPress: () -> Unit,
    onOkRelease: (Long) -> Unit,
    onCancel: () -> Unit
) {
    var showFlash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400) // 400ms flash as psychological deterrent
        showFlash = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showFlash) Color.White else BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        if (!showFlash) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null,
                    tint = Red, modifier = Modifier.size(52.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF111111))
                        .border(1.5.dp, Red.copy(0.6f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "⚠  UNAUTHORIZED ACCESS DETECTED",
                        color = Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Divider(color = Red.copy(0.4f))
                    Text(
                        "Do you still want to proceed?\n\nNote: Usage will be collected and logged.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, Color(0xFF333333)),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Cancel") }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Red.copy(0.85f))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        val pressTime = System.currentTimeMillis()
                                        onOkPress()
                                        val up = waitForUpOrCancellation()
                                        if (up != null) {
                                            val elapsed = System.currentTimeMillis() - pressTime
                                            onOkRelease(elapsed)
                                        } else {
                                            onOkRelease(0L) // Cancelled
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeterrenceScreen(onFinish: () -> Unit) {
    val steps = listOf(
        "🔐 Encrypting session data...",
        "📡 Connecting to secure server...",
        "📤 Uploading activity report...",
        "✅ Upload Complete",
        "",
        "📋 Report ID: ${System.currentTimeMillis()}"
    )
    var visibleSteps by remember { mutableStateOf(0) }
    var showFlash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        showFlash = false
        steps.forEachIndexed { i, _ ->
            kotlinx.coroutines.delay(700)
            visibleSteps = i + 1
        }
        kotlinx.coroutines.delay(2500)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showFlash) Color.White else BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        if (!showFlash) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.5.dp, Red.copy(0.6f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null,
                        tint = Red, modifier = Modifier.size(24.dp))
                    Text("SECURITY ALERT", color = Red, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, letterSpacing = 2.sp)
                }
                Divider(color = Red.copy(0.4f))

                steps.take(visibleSteps).forEach { step ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        if (step.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                        } else {
                            Text(
                                step,
                                color = if (step.startsWith("✅")) Green
                                        else Color.White.copy(0.8f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (visibleSteps < steps.size) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Red,
                        trackColor = BgElevated
                    )
                }
            }
        }
    }
}

@Composable
fun AuthorizedScreen(onViewLogs: () -> Unit, onProceed: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("✅", fontSize = 52.sp)
            Text("User Authorized!", color = Green,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Monitoring mode is OFF.\nWelcome back, owner.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewLogs,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Green),
                    border = BorderStroke(1.dp, Green.copy(0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("📋 View Log") }

                Button(
                    onClick = onProceed,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BgElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) { Text("Proceed") }
            }
        }
    }
}
