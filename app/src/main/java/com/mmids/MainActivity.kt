package com.mmids

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mmids.receivers.UnlockReceiver
import com.mmids.receivers.VolumeReceiver
import com.mmids.ui.navigation.MMIDSNavHost
import com.mmids.ui.theme.BgPrimary
import com.mmids.ui.theme.MMIDSTheme
import com.mmids.admin.MMIDSDeviceAdmin
import java.io.File

class MainActivity : ComponentActivity() {

    lateinit var dpm: DevicePolicyManager
    lateinit var adminComponent: ComponentName

    // Activity result launchers
    val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleAudioPicked(it) }
    }

    val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImagePicked(it) }
    }

    val adminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Admin result handled by checking isAdminActive */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MMIDSDeviceAdmin::class.java)

        // Register receivers
        registerReceiver(VolumeReceiver(), IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        registerReceiver(UnlockReceiver(), IntentFilter(Intent.ACTION_USER_PRESENT))

        // Adaptive layout — edge to edge + responsive
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
            android.util.Log.e("MMIDS", "File copy error: ${e.message}")
        }
    }
}
