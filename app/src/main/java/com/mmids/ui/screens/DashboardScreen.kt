package com.mmids.ui.screens

import android.content.Context
import android.content.Intent
import android.app.usage.UsageStatsManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mmids.MainActivity
import com.mmids.services.MonitoringService
import com.mmids.ui.components.*
import com.mmids.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateLogs: () -> Unit,
    onNavigateSettings: () -> Unit,
    activity: MainActivity
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isMonitoring by remember { mutableStateOf(MonitoringService.isMonitoring) }
    var hasLogs by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(true) }
    var hasOverlayPermission by remember { mutableStateOf(true) }

    // Check logs and permissions
    fun refreshStatus() {
        hasLogs = logFileHasContent(context)
        hasUsagePermission = checkUsagePermission(context)
        hasOverlayPermission = checkOverlayPermission(context)
        isMonitoring = MonitoringService.isMonitoring
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Animated card color
    val cardColor by animateColorAsState(
        targetValue = if (isMonitoring) Green else Red,
        animationSpec = tween(400), label = "card"
    )

    fun toggleMonitoring() {
        val action = if (isMonitoring) "STOP" else "START"
        val intent = Intent(activity, MonitoringService::class.java).apply { this.action = action }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            activity.startForegroundService(intent)
        } else {
            activity.startService(intent)
        }
        isMonitoring = !isMonitoring
        if (!isMonitoring) hasLogs = logFileHasContent(activity)
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Security, contentDescription = null,
                            tint = Green, modifier = Modifier.size(22.dp))
                        Text("MMIDS", color = Green, fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp, fontSize = 16.sp)
                    }
                },
                actions = {
                    StatusBadge(isActive = isMonitoring)
                    Spacer(Modifier.width(12.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Permission Warnings ──────────────────────────────
            if (!hasUsagePermission) {
                PermissionWarningCard(
                    title = "Usage Access Required",
                    description = "Needed to track which apps an intruder opens.",
                    onGrant = {
                        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
            }

            if (!hasOverlayPermission) {
                PermissionWarningCard(
                    title = "Overlay Permission Required",
                    description = "Needed to show intruder alerts over other apps.",
                    onGrant = {
                        activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${activity.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
            }

            // ── Status Card ──────────────────────────────────────
            MMIDSCard(tint = cardColor) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(
                        if (isMonitoring) Icons.Filled.Shield else Icons.Outlined.Shield,
                        contentDescription = null, tint = cardColor, modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            if (isMonitoring) "MONITORING ACTIVE" else "MONITORING INACTIVE",
                            color = cardColor, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (isMonitoring) "Activity tracking is ON" else "No active session",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Monitoring Toggle ─────────────────────────────────
            MMIDSCard {
                ToggleRow(
                    title = "Monitoring Mode",
                    subtitle = if (isMonitoring) "Tap to disable" else "Tap to enable",
                    checked = isMonitoring,
                    onToggle = { toggleMonitoring() }
                )
            }

            // ── Trigger Info ─────────────────────────────────────
            MMIDSCard {
                Text("Physical Triggers", color = Color.White.copy(0.7f),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                TriggerRow(Icons.Outlined.VolumeUp, "Vol UP (locked)", "Monitoring ON 🟢", Green)
                TriggerRow(Icons.Outlined.VolumeDown, "Vol DOWN (locked)", "Monitoring OFF 🔴", Red)
                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 8.dp))
                Text("Access", color = Color.White.copy(0.7f),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                TriggerRow(Icons.Outlined.Dialpad, "*#66437#", "Open dashboard", Blue)
                TriggerRow(Icons.Outlined.Fingerprint, "Hold OK (5s)", "Owner authorization", Purple)
            }

            // ── Log Available Banner ──────────────────────────────
            if (hasLogs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Blue.copy(0.1f))
                        .border(1.dp, Blue.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FiberNew, contentDescription = null,
                        tint = Blue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Activity Report Available", color = Blue, fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onNavigateLogs) {
                        Text("View", color = Blue)
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────
            SectionLabel("📋 LOGS")
            Spacer(Modifier.height(4.dp))
            MMIDSButton(
                label = "View Activity Logs",
                icon = Icons.Outlined.Description,
                color = TextPrimary,
                bgColor = BgCard,
                onClick = onNavigateLogs
            )
            Spacer(Modifier.height(4.dp))
            MMIDSButton(
                label = "Clear All Logs",
                icon = Icons.Outlined.DeleteOutline,
                color = Red,
                bgColor = Red.copy(0.08f),
                borderColor = Red.copy(0.3f),
                onClick = { showClearDialog = true },
                enabled = !isMonitoring
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("⚙️ APP")
            Spacer(Modifier.height(4.dp))
            MMIDSButton(
                label = "Settings",
                icon = Icons.Outlined.Settings,
                color = Green,
                bgColor = Green.copy(0.08f),
                borderColor = Green.copy(0.25f),
                onClick = onNavigateSettings
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Clear Logs Dialog ────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = BgElevated,
            title = { Text("Clear Logs?", color = TextPrimary) },
            text = { Text("Permanently delete all session logs?\nThis cannot be undone.",
                color = TextSecondary, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    clearLogs(context)
                    hasLogs = false
                    showClearDialog = false
                }) { Text("Delete", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

fun logFileHasContent(context: Context): Boolean {
    return try {
        val f = java.io.File(java.io.File(context.filesDir, ".mmids_logs"), "session_log.txt")
        f.exists() && f.readText().trim().isNotEmpty()
    } catch (_: Exception) { false }
}

fun clearLogs(context: Context) {
    try {
        java.io.File(java.io.File(context.filesDir, ".mmids_logs"), "session_log.txt")
            .writeText("")
    } catch (_: Exception) {}
}

@Composable
fun PermissionWarningCard(title: String, description: String, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Red.copy(0.1f))
            .border(1.dp, Red.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, color = TextSecondary, fontSize = 12.sp)
        }
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun checkUsagePermission(context: Context): Boolean {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60000, now)
    return stats != null && stats.isNotEmpty()
}

fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}
