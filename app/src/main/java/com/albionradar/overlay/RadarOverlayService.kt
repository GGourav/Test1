package com.albionradar.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.albionradar.AlbionRadarApp
import com.albionradar.R
import com.albionradar.data.DataManager
import com.albionradar.data.EntityManager
import com.albionradar.data.Entity
import com.albionradar.ui.RadarView
import kotlinx.coroutines.launch

class RadarOverlayService : Service(), LifecycleOwner {

    companion object {
        const val TAG = "RadarOverlayService"
        const val ACTION_SHOW = "com.albionradar.overlay.SHOW"
        const val ACTION_HIDE = "com.albionradar.overlay.HIDE"
        private const val NOTIFICATION_ID = 2
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var radarView: RadarView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val entityManager = EntityManager.getInstance()
    
    private var isShowing = false
    
    // Default overlay size
    private var overlayWidth = 300
    private var overlayHeight = 300

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (isShowing) return
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Inflate overlay layout
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_radar, null)
        
        // Get radar view reference
        radarView = overlayView?.findViewById(R.id.overlayRadarView)
        
        // Setup window parameters
        val params = createWindowParams()
        
        // Add to window
        try {
            windowManager.addView(overlayView, params)
            isShowing = true
            
            // Setup touch handling
            setupTouchHandling(params)
            
            // Start observing entities
            observeEntities()
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error showing overlay", e)
        }
    }

    private fun createWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            overlayWidth,
            overlayHeight,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastTouchTime = 0L
        
        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastTouchTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - lastTouchTime
                    if (touchDuration < 200) {
                        // Quick tap - toggle full screen mode
                        toggleSize(params)
                    }
                    true
                }
                else -> false
            }
        }
        
        // Resize handle
        val resizeHandle = overlayView?.findViewById<View>(R.id.resizeHandle)
        resizeHandle?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    overlayWidth = (event.rawX - params.x).toInt().coerceIn(150, 500)
                    overlayHeight = (event.rawY - params.y).toInt().coerceIn(150, 500)
                    params.width = overlayWidth
                    params.height = overlayHeight
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleSize(params: WindowManager.LayoutParams) {
        if (overlayWidth < 400) {
            overlayWidth = 400
            overlayHeight = 400
        } else {
            overlayWidth = 250
            overlayHeight = 250
        }
        params.width = overlayWidth
        params.height = overlayHeight
        windowManager.updateViewLayout(overlayView, params)
    }

    private fun observeEntities() {
        lifecycleScope.launch {
            entityManager.entities.collect { entities ->
                radarView?.updateEntities(entities)
            }
        }
    }

    private fun hideOverlay() {
        if (!isShowing) return
        
        try {
            overlayView?.let {
                windowManager.removeView(it)
            }
            overlayView = null
            radarView = null
            isShowing = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error hiding overlay", e)
        }
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, AlbionRadarApp.CHANNEL_OVERLAY)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Radar overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
