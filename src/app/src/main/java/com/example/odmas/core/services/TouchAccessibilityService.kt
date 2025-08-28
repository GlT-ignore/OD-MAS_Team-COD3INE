package com.example.odmas.core.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.example.odmas.MainActivity
import com.example.odmas.core.sensors.TouchSensorCollector

/**
 * Accessibility Service for intercepting touch and typing events system-wide
 * 
 * This service allows the app to monitor touch patterns and typing dynamics 
 * even when it's not in the foreground. It's essential for real behavioral security monitoring.
 */
class TouchAccessibilityService : AccessibilityService() {
    
    private lateinit var touchCollector: TouchSensorCollector
    
    // Typing pattern tracking
    private var lastKeyDownTime: Long = 0L
    private var lastKeyUpTime: Long = 0L
    private var lastKeyCode: Int = -1
    // Soft keyboard text-change tracking
    private var lastTextChangeTimeMs: Long = 0L
    private var lastTextLength: Int = 0

    // Escalation overlay control
    private var overlayReceiver: BroadcastReceiver? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    // Accessibility-based touch timing
    private var a11yTouchStartMs: Long = 0L
    
    companion object {
        private const val TAG = "TouchAccessibilityService"
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Touch accessibility service connected")
        
        // Configure service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or 
                        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                        AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                        AccessibilityEvent.TYPE_TOUCH_INTERACTION_END or
                        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_SELECTED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 100
        }
        
        serviceInfo = info
        
        // Initialize touch collector
        touchCollector = TouchSensorCollector()

        // Prepare WindowManager for accessibility overlay
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Register overlay control receiver (show/hide overlay)
        if (overlayReceiver == null) {
            overlayReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        "com.example.odmas.SHOW_OVERLAY" -> showVerificationOverlay()
                        "com.example.odmas.HIDE_OVERLAY" -> hideVerificationOverlay()
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction("com.example.odmas.SHOW_OVERLAY")
                addAction("com.example.odmas.HIDE_OVERLAY")
            }
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                registerReceiver(
                    overlayReceiver,
                    filter,
                    /* broadcastPermission */ null,
                    /* scheduler */ null,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(overlayReceiver, filter)
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event?.let { accessibilityEvent ->
                when (accessibilityEvent.eventType) {
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                        Log.d(TAG, "Touch event detected: CLICK")
                        // Process touch event for behavioral analysis
                        processTouchEvent(accessibilityEvent)
                    }
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                        Log.d(TAG, "Touch event detected: LONG_CLICK")
                        processTouchEvent(accessibilityEvent)
                    }
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                        Log.d(TAG, "Touch interaction started")
                        a11yTouchStartMs = System.currentTimeMillis()
                    }
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                        val end = System.currentTimeMillis()
                        val dwellMs = (end - a11yTouchStartMs).coerceAtLeast(50L).coerceAtMost(2000L)
                        Log.d(TAG, "Touch interaction ended dwell=${dwellMs}ms (a11y)")
                        val features = buildA11yTouchFeatures(dwellMs)
                        sendTouchDataToSecurityManager(features)
                        a11yTouchStartMs = 0L
                    }
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                        Log.d(TAG, "Text input detected")
                        processTypingEvent(accessibilityEvent)
                    }
                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                        if (accessibilityEvent.className?.contains("EditText") == true) {
                            Log.d(TAG, "Text field focused")
                        }
                    }
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        Log.d(TAG, "Window state changed - new app: ${accessibilityEvent.packageName}")
                        // Generate touch event when switching apps (indicates user interaction)
                        generateSystemTouchEvent()
                    }
                    AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                        Log.d(TAG, "View selected - generating touch event")
                        generateSystemTouchEvent()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event: ${e.message}")
        }
    }
    
    /**
     * Handle key events for typing pattern analysis
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        try {
            val currentTime = System.currentTimeMillis()
            
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    lastKeyDownTime = currentTime
                    lastKeyCode = event.keyCode
                    Log.d(TAG, "Key down: ${event.keyCode}")
                }
                KeyEvent.ACTION_UP -> {
                    if (lastKeyCode == event.keyCode && lastKeyDownTime > 0) {
                        val dwellTime = currentTime - lastKeyDownTime
                        val flightTime = if (lastKeyUpTime > 0) lastKeyDownTime - lastKeyUpTime else 0L
                        
                        Log.d(TAG, "Key up: ${event.keyCode}, dwell: ${dwellTime}ms, flight: ${flightTime}ms")
                        
                        // Create typing features and send to security manager
                        val isSpace = (event.keyCode == KeyEvent.KEYCODE_SPACE)
                        sendTypingDataToSecurityManager(dwellTime, flightTime, isSpace)
                        
                        lastKeyUpTime = currentTime
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing key event: ${e.message}")
        }
        
        return super.onKeyEvent(event)
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Touch accessibility service interrupted")
    }
    
    private fun processTouchEvent(event: AccessibilityEvent) {
        // Use a11y-derived dwell estimate when possible
        val now = System.currentTimeMillis()
        val dwellMs: Long = if (a11yTouchStartMs > 0L) {
            (now - a11yTouchStartMs).coerceAtLeast(50L).coerceAtMost(2000L)
        } else {
            150L
        }
        val features = buildA11yTouchFeatures(dwellMs)
        sendTouchDataToSecurityManager(features)
    }
    
    private fun processTypingEvent(event: AccessibilityEvent) {
        // Robust typing inference from a11y text changes when KeyEvents are not delivered
        val now: Long = System.currentTimeMillis()
        var added: Int = event.addedCount
        var removed: Int = event.removedCount

        // Snapshot length as fallback
        var snapshotLen: Int = lastTextLength
        try {
            val parts = event.text
            if (parts != null && parts.isNotEmpty()) {
                snapshotLen = parts[0].toString().length
            }
        } catch (_: Exception) {}

        if (added == 0 && removed == 0) {
            val delta = snapshotLen - lastTextLength
            if (delta > 0) added = delta else if (delta < 0) removed = -delta
        }

        val dwellMs: Long = 80L
        val flightMs: Long = if (lastTextChangeTimeMs > 0) now - lastTextChangeTimeMs else 0L

        if (added > 0) {
            var lastChar: Char? = null
            try {
                val parts = event.text
                if (parts != null && parts.isNotEmpty()) {
                    val s = parts[0].toString()
                    if (s.isNotEmpty()) lastChar = s.last()
                }
            } catch (_: Exception) {}

            repeat(added) {
                val isSpace: Boolean = (lastChar == ' ')
                Log.d(TAG, "Typing via A11y: added=1 dwell=${dwellMs}ms flight=${flightMs}ms isSpace=$isSpace")
                sendTypingDataToSecurityManager(dwellMs, flightMs, isSpace)
            }
            lastTextLength = (lastTextLength + added)
        } else if (removed > 0) {
            lastTextLength = (lastTextLength - removed).coerceAtLeast(0)
        }

        lastTextChangeTimeMs = now
    }
    
    private fun sendTouchDataToSecurityManager(features: DoubleArray) {
        // Send touch data to the security manager
        Log.d(TAG, "Sending touch features to security manager: ${features.contentToString()}")
        val intent = Intent("com.example.odmas.TOUCH_DATA").setPackage(packageName)
        intent.putExtra("features", features)
        sendBroadcast(intent)
    }

    private fun sendTypingDataToSecurityManager(dwellTime: Long, flightTime: Long, isSpace: Boolean) {
        // Send typing pattern data to the security manager
        Log.d(TAG, "Sending typing data: dwell=${dwellTime}ms, flight=${flightTime}ms, isSpace=$isSpace")
        val intent = Intent("com.example.odmas.TYPING_DATA").setPackage(packageName)
        intent.putExtra("dwellTime", dwellTime)
        intent.putExtra("flightTime", flightTime)
        intent.putExtra("isSpace", isSpace)
        sendBroadcast(intent)
    }
    
    /**
     * Generate touch event when system interactions are detected
     */
    private fun generateSystemTouchEvent() {
        val dwellMs = 150L
        val features = buildA11yTouchFeatures(dwellMs)
        Log.d(TAG, "Generated system touch event (fallback)")
        sendTouchDataToSecurityManager(features)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Touch accessibility service destroyed")
        // Cleanup overlay receiver and view if present
        runCatching { hideVerificationOverlay() }
        overlayReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
        }
        overlayReceiver = null
        windowManager = null
    }

    private fun showVerificationOverlay() {
        if (overlayView != null) return
        val wm = windowManager ?: return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }

        val container = FrameLayout(this).apply {
            setBackgroundColor(0xAA000000.toInt())
            setPadding(24, 16, 24, 16)
            val tv = TextView(this@TouchAccessibilityService).apply {
                text = "Verify identity"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
            }
            addView(tv)
            setOnClickListener {
                val i = Intent(this@TouchAccessibilityService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("trigger_biometric", true)
                }
                startActivity(i)
            }
        }

        overlayView = container
        wm.addView(container, params)
    }

    private fun hideVerificationOverlay() {
        val wm = windowManager
        val v = overlayView
        if (wm != null && v != null) {
            runCatching { wm.removeView(v) }
        }
        overlayView = null
    }

    private fun buildA11yTouchFeatures(dwellMs: Long): DoubleArray {
        val dwellSec: Double = (dwellMs.coerceAtLeast(1L).toDouble() / 1000.0).coerceIn(0.0, 2.0)
        val now: Long = System.currentTimeMillis()
        val x: Double = 0.5
        val y: Double = 0.5
        val pSeed: Int = (now % 17).toInt()
        val sSeed: Int = ((now / 3) % 19).toInt()
        val pressure: Double = (0.55 + 0.20 * ((pSeed % 11) / 10.0 - 0.5)).coerceIn(0.3, 0.9)
        val size: Double = (0.65 + 0.20 * ((sSeed % 9) / 8.0 - 0.5)).coerceIn(0.4, 0.95)
        val velocity: Double = (0.2 + (120.0 / dwellMs.coerceAtLeast(60).toDouble())).coerceIn(0.1, 1.0)
        val curvature: Double = 0.1
        val pressureVar: Double = (0.01 + 0.02 * ((pSeed % 7) / 6.0)).coerceIn(0.0, 0.1)
        val sizeVar: Double = (0.01 + 0.02 * ((sSeed % 5) / 4.0)).coerceIn(0.0, 0.1)
        val distance: Double = (velocity * dwellSec).coerceIn(0.0, 1.0)
        return doubleArrayOf(
            x, y, pressure, size, dwellSec, velocity, curvature, pressureVar, sizeVar, distance
        )
    }
}
