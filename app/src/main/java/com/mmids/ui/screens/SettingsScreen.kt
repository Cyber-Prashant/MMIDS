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
    IconOption("🛡️", "MMID Default", "com.mmids.AliasMMID"),
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
        val targetAlias = ICON_OPTIONS[index].aliasName
        
        ICON_OPTIONS.forEach { opt ->
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context, opt.aliasName),
                    if (opt.aliasName == targetAlias) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
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

            // ── App Name ──────────────────────────────────────────
            SectionLabel("🎭 APP DISGUISE — NAME")
            MMIDSCard {
                Text("Display name shown in notifications and device settings",
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
                    label = if (showNameSaved) "✓ Save Name" else "Save Name",
                    icon = Icons.Outlined.Save,
                    color = Green,
                    bgColor = Green.copy(0.15f),
                    borderColor = Green.copy(0.3f),
                    onClick = {
                        val name = appName.trim().ifEmpty { "MMID" }
                        prefs.edit().putString("disguise_app_name", name).apply()
                        showNameSaved = true
                        val intent = Intent(context, com.mmids.services.MonitoringService::class.java).apply {
                            action = "UPDATE_NOTIF"
                        }
                        context.startForegroundService(intent)
                    }
                )
            }

            // ── App Icon ──────────────────────────────────────────
            SectionLabel("🎭 APP DISGUISE — ICON")
            MMIDSCard {
                Text("Choose a preset disguise or pick a custom image", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))

                val rows = ICON_OPTIONS.chunked(4)
                rows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { opt ->
                            val idx = ICON_OPTIONS.indexOf(opt)
                            val isSel = idx == selectedIcon && customIconName == null
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
                                    .clickable { applyIconAlias(idx); customIconName = null },
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

                Spacer(Modifier.height(8.dp))
                Divider(color = BgDivider)
                Spacer(Modifier.height(12.dp))

                Text("Custom Icon from File Manager", color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (customIconName != null) Green.copy(0.12f) else BgElevated)
                            .border(1.dp, if (customIconName != null) Green.copy(0.4f) else Color.White.copy(0.08f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (customIconName != null) Icons.Filled.Image else Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null, tint = if (customIconName != null) Green else TextSecondary,
                            modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customIconName ?: "No custom icon selected", color = if (customIconName != null) Color.White else TextSecondary,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("PNG or JPG, min 192x192px", color = TextDim, fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { activity.pickImage() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue),
                        border = BorderStroke(1.dp, Blue.copy(0.3f)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) { Text("Browse", fontSize = 12.sp) }
                }

                Spacer(Modifier.height(12.dp))
                InfoBox("⚠️ After changing icon, relaunch the app. Custom icon is stored in hidden internal storage.",
                    Orange, Icons.Outlined.Info)
            }

            // ── Alert Sound ───────────────────────────────────────
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
                Spacer(Modifier.height(10.dp))
                InfoBox("🔒 Custom sound stored as hidden file — invisible in file manager",
                    Blue, Icons.Outlined.Lock)
            }

            // ── Security ──────────────────────────────────────────
            SectionLabel("🔒 SECURITY")
            MMIDSCard {
                ToggleRow(
                    title = "Start on Boot",
                    subtitle = "Auto-launch monitoring after device restart",
                    checked = autoStartEnabled,
                    onToggle = {
                        autoStartEnabled = !autoStartEnabled
                        prefs.edit().putBoolean("auto_start_boot", autoStartEnabled).apply()
                    }
                )
                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 12.dp))
                
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
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("Enable", fontSize = 11.sp) }
                    }
                }

                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 12.dp))

                StatusRow(Icons.Outlined.VisibilityOff, Purple, "Hidden from Launcher", "No icon visible")
                StatusRow(Icons.Outlined.FolderOff, Teal, "Logs Hidden", ".nomedia — invisible")
                StatusRow(Icons.Outlined.PowerOff, Color(0xFF607D8B), "Auto-Save on Shutdown", "Session preserved on power off")
                StatusRow(Icons.Outlined.VolumeUp, Blue, "Lock Screen Trigger", "Physical Volume buttons only")

                Spacer(Modifier.height(12.dp))
                InfoBox(
                    text = "When the screen is locked, only the device's physical Volume buttons will trigger capture. Volume controls from Bluetooth audio devices are intentionally ignored for security.",
                    color = Blue,
                    icon = Icons.Outlined.Info
                )
            }

            // ── Legal ─────────────────────────────────────────────
            SectionLabel("⚖️ LEGAL")
            MMIDSCard {
                LegalExpandable(
                    icon = Icons.Outlined.Gavel,
                    title = "Terms & Conditions",
                    subtitle = "Last Updated: April 2024",
                    expanded = termsExpanded,
                    onToggle = { termsExpanded = !termsExpanded }
                ) {
                    LegalText("Welcome to MMID. Use of this application is at your own risk. The developer is not responsible for any misuse. This app is for educational and security purposes only.")
                }
                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 4.dp))
                LegalExpandable(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Last Updated: April 2024",
                    expanded = privacyExpanded,
                    onToggle = { privacyExpanded = !privacyExpanded }
                ) {
                    LegalText("Your privacy is important. MMID stores all logs locally on your device. We do not collect or share any personal information with third parties.")
                }
            }

            // ── Danger Zone ───────────────────────────────────────
            SectionLabel("⚠️ DANGER ZONE")
            MMIDSButton(
                label = "Uninstall MMIDS",
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

@Composable
fun StatusRow(icon: ImageVector, color: Color, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun LegalExpandable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Blue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()) {
            content()
        }
    }
}

@Composable
fun LegalText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BgElevated)
            .padding(12.dp)
    ) {
        val scroll = rememberScrollState()
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            modifier = Modifier.verticalScroll(scroll)
        )
    }
    Spacer(Modifier.height(8.dp))
}
