package com.albionradar.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.albionradar.AlbionRadarApp
import com.albionradar.MainActivity
import com.albionradar.R
import com.albionradar.data.EntityManager
import com.albionradar.photon.PhotonParser
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AlbionVpnService : VpnService() {

    companion object {
        const val TAG = "AlbionVpnService"
        const val ACTION_CONNECT = "com.albionradar.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.albionradar.vpn.DISCONNECT"
        
        private const val ALBION_PORT = 5056
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val MTU = 1500
        private const val NOTIFICATION_ID = 1
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val photonParser = PhotonParser()
    private val entityManager = EntityManager.getInstance()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                if (!isRunning) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    connect()
                }
            }
            ACTION_DISCONNECT -> {
                disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun connect() {
        try {
            // Build VPN interface
            val builder = Builder()
                .setSession("AlbionRadar")
                .setMtu(MTU)
                .addAddress(VPN_ADDRESS, 24)
                .addRoute(VPN_ROUTE, 0)
                .addDisallowedApplication(packageName) // Exclude self
            
            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                return
            }
            
            isRunning = true
            Log.d(TAG, "VPN connected, starting packet capture")
            
            // Start packet capture coroutine
            scope.launch {
                capturePackets()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting VPN", e)
        }
    }

    private fun disconnect() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        scope.cancel()
        Log.d(TAG, "VPN disconnected")
    }

    private suspend fun capturePackets() {
        val vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)
        
        val buffer = ByteBuffer.allocate(32767)
        
        try {
            while (isRunning && vpnInterface != null) {
                // Read packet from VPN interface
                val length = vpnInput.read(buffer.array())
                if (length > 0) {
                    buffer.limit(length)
                    
                    // Parse IP header
                    val packetData = buffer.array().sliceArray(0 until length)
                    
                    // Check if this is UDP packet to Albion port
                    val albionPayload = extractAlbionPayload(packetData)
                    
                    if (albionPayload != null) {
                        // Process Photon packet
                        processPhotonPacket(albionPayload)
                    }
                    
                    // Always forward the packet (passthrough)
                    vpnOutput.write(packetData)
                    
                    buffer.clear()
                }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Log.e(TAG, "Error capturing packets", e)
            }
        }
    }

    private fun extractAlbionPayload(packet: ByteArray): ByteArray? {
        try {
            // Check IP version (first 4 bits)
            val version = (packet[0].toInt() shr 4) and 0x0F
            if (version != 4) return null // Only handle IPv4
            
            // Parse IP header
            val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
            if (packet.size < ipHeaderLength + 8) return null
            
            // Check protocol (byte 9 in IP header)
            val protocol = packet[9].toInt() and 0xFF
            if (protocol != 17) return null // 17 = UDP
            
            // Parse UDP header
            val udpHeaderStart = ipHeaderLength
            val srcPort = ((packet[udpHeaderStart].toInt() and 0xFF) shl 8) or 
                          (packet[udpHeaderStart + 1].toInt() and 0xFF)
            val dstPort = ((packet[udpHeaderStart + 2].toInt() and 0xFF) shl 8) or 
                          (packet[udpHeaderStart + 3].toInt() and 0xFF)
            
            // Check if this is Albion traffic (port 5056)
            if (srcPort != ALBION_PORT && dstPort != ALBION_PORT) return null
            
            // UDP header is 8 bytes
            val payloadStart = udpHeaderStart + 8
            if (packet.size <= payloadStart) return null
            
            return packet.sliceArray(payloadStart until packet.size)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing packet", e)
            return null
        }
    }

    private fun processPhotonPacket(payload: ByteArray) {
        try {
            val events = photonParser.parsePacket(payload)
            
            for (event in events) {
                entityManager.processEvent(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Photon packet", e)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return Notification.Builder(this, AlbionRadarApp.CHANNEL_VPN)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("VPN capture active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }
}
