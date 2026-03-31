package com.albionradar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.albionradar.data.DataManager

class AlbionRadarApp : Application() {

    companion object {
        const val CHANNEL_VPN = "vpn_service"
        const val CHANNEL_OVERLAY = "overlay_service"
        const val CHANNEL_ALERTS = "alerts"
        
        @Volatile
        private var instance: AlbionRadarApp? = null
        
        fun getInstance(): AlbionRadarApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        createNotificationChannels()
        
        // Initialize data manager
        DataManager.initialize(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // VPN Service Channel
            val vpnChannel = NotificationChannel(
                CHANNEL_VPN,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN packet capture service"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(vpnChannel)
            
            // Overlay Service Channel
            val overlayChannel = NotificationChannel(
                CHANNEL_OVERLAY,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Radar overlay display"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(overlayChannel)
            
            // Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hostile player alerts"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }
}
