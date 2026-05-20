package com.mmids

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mmids.ui.navigation.MMIDSNavHost
import com.mmids.ui.theme.BgPrimary
import com.mmids.ui.theme.MMIDSTheme
import com.mmids.admin.MMIDSDeviceAdmin
import java.io.File

class MainActivity : ComponentActivity() {

    lateinit var dpm: DevicePolicyManager
    lateinit var adminComponent: ComponentName

    val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handleAudioPicked(it) } }

    val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handleImagePicked(it) } }

    val adminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Result checked via isAdminActive */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MMIDS", "MainActivity onCreate")
        
        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MMIDSDeviceAdmin::class.java)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("user_consented", false)) {
            // Apply the chosen disguise (or hide if none selected)
            applyDisguise()
            requestBatteryExemption()
            startStandbyService()
        }

        setContent {
            MMIDSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgPrimary
                ) {
                    MMIDSNavHost(activity = this)
                }
            }
        }
    }

    fun startStandbyService() {
        try {
            val intent = Intent(this, com.mmids.services.MonitoringService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MMIDS", "Failed to start service: ${e.message}")
        }
    }

    fun applyDisguise() {
        val prefs = getSharedPreferences("mmids_prefs", MODE_PRIVATE)
        // If disguise_icon_index is -1, it means "Hidden" (all disabled)
        val selectedIndex = prefs.getInt("disguise_icon_index", 0) 
        
        val aliases = listOf(
            "com.mmids.AliasMMID", "com.mmids.AliasSettings", "com.mmids.AliasSignal",
            "com.mmids.AliasTool", "com.mmids.AliasStats", "com.mmids.AliasBattery",
            "com.mmids.AliasNetwork", "com.mmids.AliasClock", "com.mmids.AliasFiles"
        )
        
        aliases.forEachIndexed { index, alias ->
            val state = if (index == selectedIndex) 
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
            else 
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                
            try {
                packageManager.setComponentEnabledSetting(
                    ComponentName(this, alias),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                Log.e("MMIDS", "Failed to set alias $alias: ${e.message}")
            }
        }
        Log.d("MMIDS", "Disguise applied: index $selectedIndex")
    }

    fun requestBatteryExemption() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                ))
            }
        } catch (_: Exception) {}
    }

    fun enableDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Required to protect this security service from unauthorized removal."
            )
        }
        adminLauncher.launch(intent)
    }

    fun isAdminActive(): Boolean = dpm.isAdminActive(adminComponent)

    fun uninstallSelf() {
        if (dpm.isAdminActive(adminComponent)) dpm.removeActiveAdmin(adminComponent)
        startActivity(Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun pickAudio() = audioPickerLauncher.launch("audio/*")
    fun pickImage() = imagePickerLauncher.launch("image/*")

    private fun handleAudioPicked(uri: Uri) {
        copyToHiddenDir(uri, ".mmids_sounds", "shutter_custom.mp3", "shutter_sound_path")
    }

    private fun handleImagePicked(uri: Uri) {
        copyToHiddenDir(uri, ".mmids_icons", "custom_icon.png", "custom_icon_path")
    }

    private fun copyToHiddenDir(uri: Uri, dir: String, filename: String, prefKey: String) {
        try {
            val internalDir = File(filesDir, dir).also {
                if (!it.exists()) it.mkdirs()
                File(it, ".nomedia").createNewFile()
            }
            val dest = File(internalDir, filename)
            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
            getSharedPreferences("mmids_prefs", MODE_PRIVATE).edit {
                putString(prefKey, dest.absolutePath)
            }
        } catch (e: Exception) {
            Log.e("MMIDS", "File copy error: ${e.message}")
        }
    }
}
