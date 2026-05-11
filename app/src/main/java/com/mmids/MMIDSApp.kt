package com.mmids

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mmids.services.MonitoringService

class MMIDSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Persistent monitoring service channel
        manager.createNotificationChannel(
            NotificationChannel(
                MonitoringService.CHANNEL_ID,
                "MMIDS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Background monitoring service" }
        )

        // Log available alert channel
        manager.createNotificationChannel(
            NotificationChannel(
                MonitoringService.ALERT_CHANNEL_ID,
                "MMIDS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Security alerts and log notifications" }
        )
    }
}
