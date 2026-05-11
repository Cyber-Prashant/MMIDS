package com.mmids.ui.screens

import android.content.ComponentName
import android.content.Context
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
    IconOption("⚙️", "System Settings", ".AliasSettings"),
    IconOption("📡", "Signal Monitor",  ".AliasSignal"),
    IconOption("🔧", "Device Tools",    ".AliasTool"),
    IconOption("📊", "Usage Stats",     ".AliasStats"),
    IconOption("🔋", "Battery Manager", ".AliasBattery"),
    IconOption("📶", "Network Info",    ".AliasNetwork"),
    IconOption("🕐", "Clock Service",   ".AliasClock"),
    IconOption("📁", "File Manager",    ".AliasFiles"),
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

    // Legal section expanded states
    var termsExpanded by remember { mutableStateOf(false) }
    var privacyExpanded by remember { mutableStateOf(false) }

    fun applyIconAlias(index: Int) {
        val pm = context.packageManager
        ICON_OPTIONS.forEachIndexed { i, opt ->
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context, "com.mmids${opt.aliasName}"),
                    if (i == index) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}
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
            Spacer(Modifier.height(4.dp))
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
                    }
                )
            }

            // ── App Icon — Presets ────────────────────────────────
            SectionLabel("🎭 APP DISGUISE — ICON")
            Spacer(Modifier.height(4.dp))
            MMIDSCard {
                Text("Choose a preset disguise or pick a custom image from file manager",
                    color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))

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

                Divider(color = BgDivider)
                Spacer(Modifier.height(10.dp))

                // Custom icon from file manager
                Text("Custom Icon from File Manager",
                    color = Color.White.copy(0.7f), fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (customIconName != null) Green.copy(0.15f) else BgElevated)
                            .border(
                                if (customIconName != null) 1.5.dp else 0.5.dp,
                                if (customIconName != null) Green else Color.White.copy(0.07f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (customIconName != null) Icons.Filled.Image
                            else Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (customIconName != null) Green else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customIconName ?: "No custom icon selected",
                            color = if (customIconName != null) Color.White.copy(0.7f) else TextSecondary,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("PNG or JPG, min 192×192px",
                            color = TextDim, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        OutlinedButton(
                            onClick = { activity.pickImage() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue),
                            border = BorderStroke(1.dp, Blue.copy(0.3f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) { Text("Browse", fontSize = 12.sp) }
                        if (customIconName != null) {
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    prefs.edit().remove("custom_icon_path").apply()
                                    customIconName = null
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.height(24.dp)
                            ) { Text("Clear", color = Red, fontSize = 11.sp) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                InfoBox("⚠️ After changing icon, relaunch the app. " +
                    "Custom icon is stored in hidden internal storage.",
                    Orange, Icons.Outlined.Info)
            }

            // ── Shutter Sound ─────────────────────────────────────
            SectionLabel("🔊 ALERT SOUND")
            Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(8.dp))
                InfoBox("🔒 Custom sound stored as hidden file — invisible in file manager",
                    Blue, Icons.Outlined.Lock)
            }

            // ── Security ──────────────────────────────────────────
            SectionLabel("🔒 SECURITY")
            Spacer(Modifier.height(4.dp))
            MMIDSCard {
                // Auto-start on boot toggle
                ToggleRow(
                    title = "Auto-start on Boot",
                    subtitle = "Launch MMID automatically when the phone boots",
                    checked = autoStartEnabled,
                    onToggle = {
                        autoStartEnabled = !autoStartEnabled
                        prefs.edit().putBoolean("auto_start_boot", autoStartEnabled).apply()
                        // Enable/disable BootReceiver component
                        val pm = context.packageManager
                        val comp = ComponentName(context, "com.mmids.receivers.BootReceiver")
                        pm.setComponentEnabledSetting(
                            comp,
                            if (autoStartEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                )
                Divider(color = BgDivider)
                Spacer(Modifier.height(4.dp))

                // Lock trigger toggle
                ToggleRow(
                    title = "Lock Screen Trigger",
                    subtitle = "Use volume buttons to toggle monitoring while screen is locked",
                    checked = lockTriggerEnabled,
                    onToggle = {
                        lockTriggerEnabled = !lockTriggerEnabled
                        prefs.edit().putBoolean("lock_trigger", lockTriggerEnabled).apply()
                    }
                )

                // Physical only info box
                Spacer(Modifier.height(10.dp))
                InfoBox(
                    text = "Volume buttons only — When screen is locked, ONLY the physical " +
                        "volume buttons on the device function as triggers. " +
                        "Bluetooth audio controls and screen unlock events will NOT trigger monitoring.",
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
                Divider(color = BgDivider)
                SecurityRow(Icons.Outlined.VisibilityOff, Purple,
                    "Hidden from Launcher", "No icon visible to others") {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = Green, modifier = Modifier.size(20.dp))
                }
                Divider(color = BgDivider)
                SecurityRow(Icons.Outlined.FolderOff, Teal,
                    "Logs Hidden", ".nomedia — invisible in file manager") {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = Green, modifier = Modifier.size(20.dp))
                }
                Divider(color = BgDivider)
                SecurityRow(Icons.Outlined.PowerOff, Color(0xFF607D8B),
                    "Auto-Save on Shutdown", "Session preserved on power off") {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = Green, modifier = Modifier.size(20.dp))
                }
            }

            // ── Legal ─────────────────────────────────────────────
            SectionLabel("⚖️ LEGAL")
            Spacer(Modifier.height(4.dp))
            MMIDSCard {
                // Terms & Conditions
                LegalExpandable(
                    icon = Icons.Outlined.Gavel,
                    title = "Terms & Conditions",
                    expanded = termsExpanded,
                    onToggle = { termsExpanded = !termsExpanded }
                ) {
                    LegalText("""
Welcome to this application. By installing or using this app, you agree to the following terms and conditions.

1. Use of the Application
This application is intended for educational, personal, and ethical use only. You agree to use the app in compliance with all applicable laws and regulations.

2. No Responsibility for Misuse
The developer of this application shall not be held responsible for any misuse, illegal activity, or unauthorized use of this app. Any actions performed using this application are solely the responsibility of the user. You agree that you are using this app at your own risk.

3. User Responsibility
You are responsible for:
• How you use this application
• Any data collected, monitored, or shared through it
• Ensuring that your use does not violate privacy, security, or legal boundaries

4. No Warranty
This application is provided "as is", without any warranties or guarantees of any kind. The developer does not guarantee:
• Accuracy of data
• Continuous availability
• Error-free performance

5. Changes to Terms
These Terms & Conditions may be updated or modified at any time without prior notice. Continued use of the app means you accept any changes.

6. Developer Information
This application was created and developed by:
Prashant Piyush
All rights reserved.

7. Acceptance
By using this application, you confirm that you have read, understood, and agreed to these Terms & Conditions.
                    """.trimIndent())
                }

                Divider(color = BgDivider, modifier = Modifier.padding(vertical = 4.dp))

                // Privacy Policy
                LegalExpandable(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    expanded = privacyExpanded,
                    onToggle = { privacyExpanded = !privacyExpanded }
                ) {
                    LegalText("""
This Privacy Policy describes how this application collects, uses, and protects your information.

1. Information We Collect
This app may collect the following types of information:
• App Usage Data (e.g., app names, usage duration)
• Device Activity Logs (e.g., timestamps of events)
• Basic Device Information (only if required for functionality)

We do not collect:
• Personal messages
• Passwords
• Sensitive personal content

2. How We Use Information
Collected data is used solely to:
• Provide security monitoring features
• Detect unusual or suspicious activity
• Display logs and insights within the app

3. Data Storage
• All data is stored locally on your device
• We do not upload or share your data to any external server

4. Data Sharing
We do not sell, share, or transfer your data to third parties. Data may only be shared if:
• You manually choose to export it
• Required by law

5. Permissions Usage
This app may request permissions such as:
• Usage Access – to monitor app activity
• Device Admin – for device security features
Each permission is used only for its stated purpose and never for hidden tracking.

6. User Control & Rights
You have full control over your data:
• You can disable monitoring anytime
• You can revoke permissions via device settings
• You can uninstall the app to remove all stored data

7. Security
We take reasonable measures to protect your data, including secure local storage practices.

8. Children's Privacy
This app is not intended for use by children without supervision. Users are responsible for ensuring lawful usage.

9. Changes to This Policy
This policy may be updated from time to time. Continued use of the app indicates acceptance of any updates.

10. Developer Information
This application was created and developed by:
Prashant Piyush

11. Contact
For any questions or concerns regarding this policy, please contact the developer through official channels provided with the app.
                    """.trimIndent())
                }
            }

            // ── Contact ───────────────────────────────────────────
            SectionLabel("📬 CONTACT & SUPPORT")
            Spacer(Modifier.height(4.dp))
            MMIDSCard {
                ContactRow(
                    icon = Icons.Outlined.Send,
                    platform = "Telegram",
                    handle = "@PrashantCyberCore",
                    color = Color(0xFF2196F3),
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                Uri.parse("https://t.me/PrashantCyberCore")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
                Divider(color = BgDivider)
                ContactRow(
                    icon = Icons.Outlined.Email,
                    platform = "Email",
                    handle = "cybercore.support@rediffmail.com",
                    color = Color(0xFFFF5722),
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_SENDTO,
                                Uri.parse("mailto:cybercore.support@rediffmail.com")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }

            // ── Danger Zone ───────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            SectionLabel("⚠️ DANGER ZONE")
            Spacer(Modifier.height(4.dp))
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
                Text("This will:\n\n• Stop all monitoring\n• Remove device admin\n• Delete all logs\n• Uninstall the app",
                    color = TextSecondary, lineHeight = 22.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    clearLogs(context)
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

// ── Legal Expandable ─────────────────────────────────────────────
@Composable
fun LegalExpandable(
    icon: ImageVector,
    title: String,
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
            Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
            Text(title, color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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

// ── Legal Text Block ─────────────────────────────────────────────
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

// ── Contact Row ───────────────────────────────────────────────────
@Composable
fun ContactRow(
    icon: ImageVector,
    platform: String,
    handle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(platform, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(handle, color = color, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.OpenInNew, contentDescription = null,
            tint = TextDim, modifier = Modifier.size(16.dp))
    }
}
