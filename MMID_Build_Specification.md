# ============================================================
# MMID — Manual Mobile Intrusion Detection System
# COMPLETE BUILD SPECIFICATION PROMPT
# Developer: Prashant Piyush
# Use this prompt with any AI to rebuild the full app
# ============================================================

## PROMPT TO PASTE INTO ANY AI:

---

Build me a complete Android application in **Jetpack Compose + Kotlin** called **MMID (Manual Mobile Intrusion Detection System)**.

---

## TECH STACK

- Language: Kotlin
- UI: Jetpack Compose (Material3)
- Min SDK: 26 | Target SDK: 34
- Build: Gradle 8.2 + Kotlin 1.9.22
- Package name: com.mmids
- App label: MMID

### Dependencies (app/build.gradle):
```
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
androidx.activity:activity-compose:1.8.2
androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0
compose-bom:2024.01.00
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.compose.material:material-icons-extended
androidx.compose.animation:animation
androidx.navigation:navigation-compose:2.7.6
kotlinx-coroutines-android:1.7.3
accompanist-systemuicontroller:0.32.0
```

---

## PROJECT STRUCTURE

```
app/src/main/
├── AndroidManifest.xml
├── java/com/mmids/
│   ├── MMIDSApp.kt
│   ├── MainActivity.kt
│   ├── admin/MMIDSDeviceAdmin.kt
│   ├── receivers/Receivers.kt         (UnlockReceiver, DialerReceiver, ShutdownReceiver, BootReceiver)
│   ├── services/
│   │   ├── MonitoringService.kt
│   │   └── AppUsageTracker.kt
│   └── ui/
│       ├── theme/Theme.kt
│       ├── components/Components.kt
│       ├── navigation/MMIDSNavHost.kt
│       └── screens/
│           ├── ConsentScreen.kt
│           ├── DashboardScreen.kt
│           ├── LogViewerScreen.kt
│           ├── SettingsScreen.kt
│           └── IntruderAlertActivity.kt
└── res/
    ├── raw/shutter.mp3
    ├── xml/device_admin_policies.xml
    ├── values/themes.xml
    └── mipmap-*/ic_launcher.png (all density sizes)
```

---

## PERMISSIONS (AndroidManifest.xml)

```xml
PACKAGE_USAGE_STATS
FOREGROUND_SERVICE
FOREGROUND_SERVICE_DATA_SYNC
SYSTEM_ALERT_WINDOW
VIBRATE
MODIFY_AUDIO_SETTINGS
RECEIVE_BOOT_COMPLETED
POST_NOTIFICATIONS
PROCESS_OUTGOING_CALLS
READ_MEDIA_AUDIO
READ_MEDIA_IMAGES
READ_EXTERNAL_STORAGE (maxSdkVersion=32)
BIND_DEVICE_ADMIN
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
QUERY_ALL_PACKAGES
USE_FULL_SCREEN_INTENT
```

---

## FEATURE 1 — USER CONSENT SCREEN (First Launch Only)

- Shown ONLY on first launch using SharedPreferences key `user_consented`
- After "I Agree" → save consent → call `hideFromLauncher()` → navigate to Dashboard
- "Exit App" button → show confirmation dialog → `finishAffinity()`
- Content includes: what app does, permissions, auto-start, hidden icon, lock screen trigger, developer info
- After consent, never shown again

---

## FEATURE 2 — MONITORING SERVICE (MonitoringService.kt)

- Foreground service with `START_STICKY` (restarts if killed)
- `foregroundServiceType="dataSync"` in manifest
- `stopWithTask="false"` so it survives app close

### Volume Detection (CRITICAL — use ContentObserver, NOT broadcast):
```kotlin
// Register ContentObserver on Settings.System.CONTENT_URI
// This works on ALL Android versions including 8+
// VOLUME_CHANGED_ACTION broadcast does NOT work on Android 8+ via manifest
val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) return  // ONLY when screen is locked
        val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("lock_trigger", true)) return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val cur = am.getStreamVolume(AudioManager.STREAM_RING)
        // Volume UP = monitoring ON, Volume DOWN = monitoring OFF
    }
}
contentResolver.registerContentObserver(
    android.provider.Settings.System.CONTENT_URI, true, volumeObserver
)
```

### UnlockReceiver Registration (CRITICAL — dynamic only):
```kotlin
// Must be registered DYNAMICALLY in service onCreate()
// ACTION_USER_PRESENT cannot be reliably received via manifest on Android 8+
registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
```

### Session Logging:
- Logs stored in `filesDir/.mmids_logs/session_log.txt`
- Create `.nomedia` file in that dir so it's hidden from file managers
- Format:
```
═══════════════════════════════════════
SESSION START: 2025-01-15 22:10:00
═══════════════════════════════════════
  [APP OPENED] WhatsApp @ 22:10:34
  [APP OPENED] Chrome @ 22:11:02
───────────────────────────────────────
SESSION END: 22:15:00 | Reason: MANUAL
═══════════════════════════════════════
```
- On device shutdown: write `Reason: DEVICE_SHUTDOWN`

---

## FEATURE 3 — APP USAGE TRACKER (AppUsageTracker.kt)

- Uses `UsageStatsManager` with `UsageEvents.Event.MOVE_TO_FOREGROUND`
- Polls every 1500ms in a coroutine
- Checks `hasUsagePermission()` before starting
- Filters out own package, systemui, launcher packages
- API 33+ fix: use `PackageManager.ApplicationInfoFlags.of(0)` for `getApplicationInfo`

```kotlin
// Permission check
fun hasUsagePermission(): Boolean {
    val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60000, now)
    return stats != null && stats.isNotEmpty()
}
```

---

## FEATURE 4 — INTRUDER ALERT ACTIVITY (IntruderAlertActivity.kt)

### Window Flags (CRITICAL for showing over lock screen):
```kotlin
window.addFlags(
    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
)
setShowWhenLocked(true)  // API 27+
setTurnScreenOn(true)    // API 27+
```

### Manifest entry:
```xml
<activity
    android:name=".ui.screens.IntruderAlertActivity"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:launchMode="singleTask"
    android:excludeFromRecents="true"
    android:screenOrientation="portrait"/>
```

### Triggered by:
- `UnlockReceiver` when screen unlocks
- Mode "PROMPT" if monitoring is OFF
- Mode "DETERRENCE" if monitoring is ON

### PROMPT screen:
- Shows "⚠ UNAUTHORIZED ACCESS DETECTED"
- Shows "Do you still want to proceed? Note: Usage will be collected and logged."
- Two buttons: Cancel (locks screen) and OK (silent long press detection)
- **NO hint text, NO progress bar visible to intruder**
- White screen flash for 180ms on load

### OK Button Long Press (CRITICAL — use awaitPointerEventScope):
```kotlin
.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent(PointerEventPass.Main)
            if (down.type == PointerEventType.Press) {
                val pressTime = System.currentTimeMillis()
                // Start 5-second handler
                holdHandler?.postDelayed(holdRunnable!!, 5000L)
                while (true) {
                    val up = awaitPointerEvent(PointerEventPass.Main)
                    if (up.type == PointerEventType.Release) {
                        val elapsed = System.currentTimeMillis() - pressTime
                        holdHandler?.removeCallbacks(holdRunnable!!)
                        if (!authorized && elapsed < 5000L) onIntruder()
                        break
                    }
                }
            }
        }
    }
}
```
- Short press OK → intruder confirmed → monitoring ON + deterrence sequence
- Hold OK 5 seconds → owner authorized (NO visual indicator whatsoever)
- Cancel → lock screen via `DevicePolicyManager.lockNow()`

### DETERRENCE sequence (after short-press OK or if monitoring already ON):
1. White screen flash (180ms)
2. Play shutter sound at max volume
3. Show animated terminal sequence:
   - "🔐 Encrypting session data..."
   - "📡 Connecting to secure server..."
   - "📤 Uploading activity report..."
   - "✅ Upload Complete"
   - "📋 Report ID: [timestamp]"
4. Auto-dismiss after 2.5s after sequence ends

### AUTHORIZED screen (after 5s hold):
- Shows "✅ User Authorized!"
- "Welcome back, owner."
- Two buttons: "📋 View Log" | "Proceed"
- Stops monitoring if it was ON

---

## FEATURE 5 — DASHBOARD SCREEN (DashboardScreen.kt)

- Status card (red when OFF, green when ON, animated color transition)
- Monitoring toggle switch
- Physical triggers info card (Vol UP = ON, Vol DOWN = OFF, both locked only)
- Access methods card (*#66437# dialer code)
- "New Activity Report Available" banner when logs exist
- View Logs button → LogViewerScreen
- Clear Logs button (disabled while monitoring active, confirmation dialog)
- Settings button
- Warning banner if UsageStats permission not granted → opens Settings.ACTION_USAGE_ACCESS_SETTINGS

---

## FEATURE 6 — LOG VIEWER SCREEN (LogViewerScreen.kt)

- Terminal-style display (monospace font, green #00FF41 on black #080808)
- Stats bar: session count + app event count + "🔒 Hidden" badge
- Scrollable SelectableText log content
- Refresh button
- Clear logs button with confirmation dialog
- Logs read from `filesDir/.mmids_logs/session_log.txt`

---

## FEATURE 7 — SETTINGS SCREEN (SettingsScreen.kt)

### App Name Section:
- OutlinedTextField for custom name
- "Save Name" button → saves to SharedPreferences → restarts service so notification updates immediately

### App Icon Section:
- 8 preset icons in 4-column grid:
  - ⚙️ System Settings → alias: com.mmids.AliasSettings
  - 📡 Signal Monitor  → alias: com.mmids.AliasSignal
  - 🔧 Device Tools    → alias: com.mmids.AliasTool
  - 📊 Usage Stats     → alias: com.mmids.AliasStats
  - 🔋 Battery Manager → alias: com.mmids.AliasBattery
  - 📶 Network Info    → alias: com.mmids.AliasNetwork
  - 🕐 Clock Service   → alias: com.mmids.AliasClock
  - 📁 File Manager    → alias: com.mmids.AliasFiles
- Each alias has its own icon resource (ic_alias_settings, ic_alias_signal, etc.)
- Custom icon picker from file manager (Browse button)
- Icon switching via:
```kotlin
pm.setComponentEnabledSetting(
    ComponentName.unflattenFromString("com.mmids.AliasSettings")!!,
    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
    PackageManager.DONT_KILL_APP
)
```

### Alert Sound Section:
- Default: `res/raw/shutter.mp3`
- Change button → file picker for audio files
- Custom sound stored in `filesDir/.mmids_sounds/shutter_custom.mp3` with `.nomedia`
- Reset to default option

### Security Section:
- Toggle: Auto-start on Boot (enables/disables BootReceiver component)
- Toggle: Lock Screen Trigger (saved to prefs, checked by ContentObserver)
- Info box: "Physical buttons ONLY — Bluetooth audio controls will NOT trigger monitoring"
- Device Admin enable button
- Status rows: Hidden from Launcher ✓, Logs Hidden ✓, Auto-Save on Shutdown ✓

### Legal Section:
- Expandable Terms & Conditions (full text)
- Expandable Privacy Policy (full text)
- Both by developer: Prashant Piyush

### Contact Section:
- Telegram: @PrashantCyberCore → opens t.me/PrashantCyberCore
- Email: cybercore.support@rediffmail.com → opens mailto intent

### Danger Zone:
- "Uninstall MMID" button → confirmation dialog → removes admin → uninstalls

---

## FEATURE 8 — LAUNCHER HIDING (MainActivity.kt)

### CRITICAL — use full package+class format for ComponentName:
```kotlin
// WRONG:  ComponentName(context, ".AliasSettings")
// CORRECT: ComponentName.unflattenFromString("com.mmids.AliasSettings")!!

fun hideFromLauncher() {
    listOf(
        "com.mmids.AliasSettings", "com.mmids.AliasSignal", "com.mmids.AliasTool",
        "com.mmids.AliasStats", "com.mmids.AliasBattery", "com.mmids.AliasNetwork",
        "com.mmids.AliasClock", "com.mmids.AliasFiles"
    ).forEach { alias ->
        packageManager.setComponentEnabledSetting(
            ComponentName.unflattenFromString(alias)!!,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
```

- Called immediately when user clicks "I Agree" on consent screen
- Called again via `LaunchedEffect` on every subsequent launch
- App accessible only via dialer code `*#66437#`

---

## FEATURE 9 — SECRET ACCESS (DialerReceiver.kt)

```kotlin
// Dial *#66437# to open hidden app
if (number.trim() == "*#66437#") {
    resultData = null  // cancel the call
    context.startActivity(Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    })
}
```

---

## FEATURE 10 — BOOT RECEIVER (BootReceiver.kt)

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("user_consented", false)) return
        if (!prefs.getBoolean("auto_start_boot", true)) return
        context.startForegroundService(Intent(context, MonitoringService::class.java))
    }
}
```

---

## THEME (Theme.kt)

```kotlin
// Background
BgPrimary   = Color(0xFF0A0A0A)
BgCard      = Color(0xFF141414)
BgElevated  = Color(0xFF1A1A1A)
BgDivider   = Color(0xFF1E1E1E)
BgInput     = Color(0xFF1A1A1A)

// Brand
Green       = Color(0xFF4CAF50)
Red         = Color(0xFFF44336)
Blue        = Color(0xFF2196F3)
Orange      = Color(0xFFFF9800)
Purple      = Color(0xFF9C27B0)
Teal        = Color(0xFF009688)

// Text
TextPrimary   = Color(0xFFFFFFFF)
TextSecondary = Color(0xFF888888)
TextDim       = Color(0xFF555555)
TextTerminal  = Color(0xFF00FF41)  // green terminal text
```

---

## MANIFEST ALIASES (all 8, in AndroidManifest.xml)

```xml
<!-- First one enabled=true by default, rest enabled=false -->
<activity-alias android:name=".AliasSettings"
    android:targetActivity=".MainActivity"
    android:enabled="true" android:exported="true"
    android:label="System Settings"
    android:icon="@mipmap/ic_alias_settings">
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>
</activity-alias>
<!-- Repeat for: AliasSignal, AliasTool, AliasStats, AliasBattery, AliasNetwork, AliasClock, AliasFiles -->
<!-- All with enabled="false" except AliasSettings -->
```

---

## BATTERY OPTIMIZATION (MainActivity.kt)

```kotlin
// Request exemption so service is never killed
val pm = getSystemService(POWER_SERVICE) as PowerManager
if (!pm.isIgnoringBatteryOptimizations(packageName)) {
    startActivity(Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:$packageName")
    ))
}
```

---

## SIGNING CONFIG (app/build.gradle)

```groovy
def keystoreProps = new Properties()
def keystoreFile = rootProject.file("key.properties")
if (keystoreFile.exists()) keystoreProps.load(new FileInputStream(keystoreFile))

signingConfigs {
    release {
        storeFile file(keystoreProps['storeFile'] ?: 'keystore.jks')
        storePassword keystoreProps['storePassword'] ?: ''
        keyAlias keystoreProps['keyAlias'] ?: ''
        keyPassword keystoreProps['keyPassword'] ?: ''
    }
}
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        signingConfig signingConfigs.release
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

---

## PROGUARD RULES

```
-keep class com.mmids.** { *; }
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.app.admin.DeviceAdminReceiver
-keep class androidx.compose.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn androidx.**
```

---

## DEVELOPER INFO

- Developer: Prashant Piyush
- App Name: MMID
- Package: com.mmids
- Telegram: @PrashantCyberCore
- Email: cybercore.support@rediffmail.com

---

## IMPORTANT IMPLEMENTATION NOTES

1. **Volume detection** → ContentObserver ONLY (NOT VOLUME_CHANGED_ACTION broadcast)
2. **UnlockReceiver** → Dynamic registration in MonitoringService.onCreate() ONLY
3. **ComponentName** → Always use `ComponentName.unflattenFromString("com.mmids.AliasX")`
4. **Intruder overlay** → Must have all 5 WindowManager flags + setShowWhenLocked(true)
5. **OK button** → Use `awaitPointerEventScope` with timestamp, NOT `detectTapGestures`
6. **App tracking** → Check `hasUsagePermission()` before starting tracker
7. **Launcher hiding** → Call BOTH on "I Agree" click AND in `LaunchedEffect(hasConsented)`
8. **Service restart** → Use `START_STICKY` and `stopWithTask="false"` in manifest
9. **Log storage** → `filesDir/.mmids_logs/` with `.nomedia` file to hide from file managers
10. **Shutter sound** → Default in `res/raw/shutter.mp3`, custom in `filesDir/.mmids_sounds/`

---

## BUILD COMMAND

```bash
chmod +x gradlew
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
adb install app/build/outputs/apk/release/app-release.apk
```

---
END OF SPECIFICATION
