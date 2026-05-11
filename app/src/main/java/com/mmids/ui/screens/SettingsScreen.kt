package com.mmids.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmids.MainActivity
import com.mmids.ui.components.*
import com.mmids.ui.theme.*

data class IconOption(val emoji: String, val label: String, val aliasName: String)

val ICON_OPTIONS = listOf(
    IconOption("⚙️", "System Settings", "com.mmids.AliasSettings"),
    IconOption("📡", "Signal Monitor",  "com.mmids.AliasSignal"),
    IconOption("🔧", "Device Tools",    "com.mmids.AliasTool"),
    IconOption("📊", "Usage Stats",     "com.mmids.AliasStats"),
    IconOption("🔋", "Battery Manager", "com.mmids.AliasBattery"),
    IconOption("📶", "Network Info",    "com.mmids.AliasNetwork"),
    IconOption("🕐", "Clock Service",   "com.mmids.AliasClock"),
    IconOption("📁", "File Manager",    "com.mmids.AliasFiles"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, activity: MainActivity) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)

    // ── State ────────────────────────────────────────────────────
    var appName by remember {
        mutableStateOf(prefs.getString("disguise_app_name", "MMID") ?: "MMID")
    }
    var selectedIcon by remember { mutableStateOf(prefs.getInt("disguise_icon_index", 0)) }
    var customIconName by remember {
        mutableStateOf(prefs.getString("custom_icon_path", "")?.let {
            if (it.isNotEmpty()) it.split("/").last() else null
        })
    }
    var shutterSoundName by remember {
        mutableStateOf(
            prefs.getString("shutter_sound_path", "")?.let {
                if (it.isNotEmpty()) it.split("/").last() else "freesound_community-camera-shutter (Default)"
            } ?: "freesound_community-camera-shutter (Default)"
        )
    }
    var autoStartEnabled by remember { mutableStateOf(prefs.getBoolean("auto_start_boot", true)) }
    var lockTriggerEnabled by remember { mutableStateOf(prefs.getBoolean("lock_trigger", true)) }
    var isAdminActive by remember { mutableStateOf(activity.isAdminActive()) }
    var showNameSaved by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    var termsExpanded by remember { mutableStateOf(false) }
    var privacyExpanded by remember { mutableStateOf(false) }

    fun applyIconAlias(index: Int) {
        val pm = context.packageManager
        ICON_OPTIONS.forEachIndexed { i, opt ->
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context, opt.aliasName),
                    if (i == index) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                android.util.Log.e("MMIDS", "Failed to set alias ${opt.aliasName}: ${e.message}")
            }
        }
        prefs.edit().putInt("disguise_icon_index", index).apply()
        selectedIcon = index
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Settings, contentDescription = null,
                            tint = Green, modifier = Modifier.size(18.dp))
                        Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
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

            SectionLabel("🎭 APP DISGUISE — NAME")
            MMIDSCard {
                Text("Displayed in notifications and device settings",
                    color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it; showNameSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. MMID", color = TextDim) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Label, contentDescription = null,
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgInput,
                        cursorColor = Green
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(10.dp))
                MMIDSButton(
                    label = if (showNameSaved) "✓ Saved!" else "Save Name",
                    icon = Icons.Outlined.Save,
                    color = Green,
                    bgColor = Green.copy(0.15f),
                    borderColor = Green.copy(0.3f),
                    onClick = {
                        val name = appName.trim().ifEmpty { "MMID" }
                        prefs.edit().putString("disguise_app_name", name).apply()
                        showNameSaved = true
                        // Trigger notification update in service
                        val intent = Intent(context, com.mmids.services.MonitoringService::class.java).apply {
                            action = "UPDATE_NOTIF"
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                )
            }

            SectionLabel("🎭 APP DISGUISE — ICON")
            MMIDSCard {
                Text("Choose a preset disguise icon", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))

                val rows = ICON_OPTIONS.chunked(4)
                rows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { opt ->
                            val idx = ICON_OPTIONS.indexOf(opt)
                            val isSel = idx == selectedIcon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Green.copy(0.15f) else BgElevated)
                                    .border(
                                        if (isSel) 1.5.dp else 0.5.dp,
                                        if (isSel) Green else Color.White.copy(0.07f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { applyIconAlias(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(4.dp)) {
                                    Text(opt.emoji, fontSize = 22.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(opt.label,
                                        color = if (isSel) Green else TextSecondary,
                                        fontSize = 8.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                InfoBox("⚠️ After changing icon, relaunch the app to see effects.",
                    Orange, Icons.Outlined.Info)
            }

            SectionLabel("🔊 ALERT SOUND")
            MMIDSCard {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Blue.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Audiotrack, contentDescription = null,
                            tint = Blue, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shutter Sound", color = TextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.Medium)
                        Text(shutterSoundName, color = TextSecondary,
                            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { activity.pickAudio() }) {
                        Text("Change", color = Blue, fontSize = 13.sp)
                    }
                }
                if (!shutterSoundName.contains("Default")) {
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            prefs.edit().remove("shutter_sound_path").apply()
                            shutterSoundName = "freesound_community-camera-shutter (Default)"
                        }) {
                            Text("Reset to default", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            SectionLabel("🔒 SECURITY")
            MMIDSCard {
                ToggleRow(
                    title = "Auto-start on Boot",
                    subtitle = "Launch MMID automatically when the phone boots",
                    checked = autoStartEnabled,
                    onToggle = {
                        autoStartEnabled = !autoStartEnabled
                        prefs.edit().putBoolean("auto_start_boot", autoStartEnabled).apply()
                    }
                )
                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = "Lock Screen Trigger",
                    subtitle = "Use volume buttons to toggle monitoring while screen is locked",
                    checked = lockTriggerEnabled,
                    onToggle = {
                        lockTriggerEnabled = !lockTriggerEnabled
                        prefs.edit().putBoolean("lock_trigger", lockTriggerEnabled).apply()
                    }
                )

                Spacer(Modifier.height(10.dp))
                InfoBox(
                    text = "When screen is locked, use physical volume buttons to toggle monitoring. " +
                        "Volume UP = ON, Volume DOWN = OFF.",
                    color = Orange,
                    icon = Icons.Outlined.VolumeUp
                )

                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 10.dp))

                SecurityRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    color = Orange,
                    title = "Device Admin Protection",
                    subtitle = "Blocks unauthorized uninstall"
                ) {
                    if (isAdminActive) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null,
                            tint = Green, modifier = Modifier.size(20.dp))
                    } else {
                        OutlinedButton(
                            onClick = {
                                activity.enableDeviceAdmin()
                                isAdminActive = activity.isAdminActive()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Orange),
                            border = BorderStroke(1.dp, Orange.copy(0.4f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) { Text("Enable", fontSize = 11.sp) }
                    }
                }
            }

            SectionLabel("⚠️ DANGER ZONE")
            MMIDSButton(
                label = "Uninstall MMID",
                icon = Icons.Outlined.DeleteForever,
                color = Color(0xFFEF9A9A),
                bgColor = Color(0xFF2A0A0A),
                borderColor = Red.copy(0.35f),
                onClick = { showUninstallDialog = true }
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showUninstallDialog) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            containerColor = BgElevated,
            title = { Text("Uninstall MMID?", color = TextPrimary) },
            text = {
                Text("This will stop all monitoring and remove the application.",
                    color = TextSecondary, lineHeight = 22.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    activity.uninstallSelf()
                }) { Text("Uninstall", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
