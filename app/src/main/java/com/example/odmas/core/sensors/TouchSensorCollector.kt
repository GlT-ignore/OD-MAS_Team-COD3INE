package com.example.odmas.core.sensors

import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * Touch Sensor Collector: Captures touch dynamics for behavioral biometrics
 * 
 * Features extracted:
 * - Touch coordinates (x, y)
 * - Touch pressure
 * - Touch size
 * - Velocity and acceleration
 * - Touch duration (dwell time)
 * - Touch curvature
 */
class TouchSensorCollector {
    
    private val _touchFeatures = MutableStateFlow<TouchFeatures?>(null)
    val touchFeatures: StateFlow<TouchFeatures?> = _touchFeatures.asStateFlow()
    
    private var lastTouchTime: Long = 0L
    private var touchStartTime: Long = 0L
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastVelocityX: Float = 0f
    private var lastVelocityY: Float = 0f
    
    private val touchHistory = mutableListOf<TouchPoint>()
    private val maxHistorySize = 10
    
    companion object {
        private const val MIN_TOUCH_DURATION_MS = 50L
        private const val MAX_TOUCH_DURATION_MS = 2000L
    }
    
    /**
     * Process touch event
     * @param event MotionEvent from the view
     * @param viewWidth Width of the view
     * @param viewHeight Height of the view
     */
    fun processTouchEvent(event: MotionEvent, viewWidth: Int, viewHeight: Int): Unit {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(event, viewWidth, viewHeight)
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(event, viewWidth, viewHeight)
            }
            MotionEvent.ACTION_UP -> {
                handleTouchUp(event, viewWidth, viewHeight)
            }
        }
    }
    
    private fun handleTouchDown(event: MotionEvent, viewWidth: Int, viewHeight: Int): Unit {
        touchStartTime = System.currentTimeMillis()
        lastTouchTime = touchStartTime
        
        val x = event.x / viewWidth // Normalize to 0-1
        val y = event.y / viewHeight // Normalize to 0-1
        
        lastX = x
        lastY = y
        lastVelocityX = 0f
        lastVelocityY = 0f
        
        touchHistory.clear()
        touchHistory.add(TouchPoint(x, y, event.pressure, event.size, touchStartTime))
    }
    
    private fun handleTouchMove(event: MotionEvent, viewWidth: Int, viewHeight: Int): Unit {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastTouchTime
        
        if (deltaTime < 16) { // Skip if less than 16ms (60fps)
            return
        }
        
        val x = event.x / viewWidth
        val y = event.y / viewHeight
        
        // Calculate velocity
        val velocityX = (x - lastX) / (deltaTime / 1000f)
        val velocityY = (y - lastY) / (deltaTime / 1000f)
        
        // Calculate acceleration
        val accelerationX = (velocityX - lastVelocityX) / (deltaTime / 1000f)
        val accelerationY = (velocityY - lastVelocityY) / (deltaTime / 1000f)
        
        // Add to history
        touchHistory.add(TouchPoint(x, y, event.pressure, event.size, currentTime))
        if (touchHistory.size > maxHistorySize) {
            touchHistory.removeAt(0)
        }
        
        // Update last values
        lastX = x
        lastY = y
        lastVelocityX = velocityX
        lastVelocityY = velocityY
        lastTouchTime = currentTime
    }
    
    private fun handleTouchUp(event: MotionEvent, viewWidth: Int, viewHeight: Int): Unit {
        val touchEndTime = System.currentTimeMillis()
        val touchDuration = touchEndTime - touchStartTime
        
        // Only process if touch duration is reasonable
        if (touchDuration < MIN_TOUCH_DURATION_MS || touchDuration > MAX_TOUCH_DURATION_MS) {
            return
        }
        
        val x = event.x / viewWidth
        val y = event.y / viewHeight
        
        // Calculate touch features
        val features = calculateTouchFeatures(x, y, event.pressure, event.size, touchDuration)
        
        // Emit features
        _touchFeatures.value = features
    }
    
    private fun calculateTouchFeatures(
        finalX: Float,
        finalY: Float,
        finalPressure: Float,
        finalSize: Float,
        duration: Long
    ): TouchFeatures {
        
        // Basic features
        val dwellTime = duration.toFloat() / 1000f // Convert to seconds
        val pressure = finalPressure
        val size = finalSize
        
        // Distance and velocity
        val distance = sqrt((finalX - touchHistory.first().x) * (finalX - touchHistory.first().x) + (finalY - touchHistory.first().y) * (finalY - touchHistory.first().y))
        val velocity = if (duration > 0) distance / (duration / 1000f) else 0f
        
        // Curvature (simplified)
        val curvature = calculateCurvature()
        
        // Pressure variance
        val pressureVariance = calculatePressureVariance()
        
        // Size variance
        val sizeVariance = calculateSizeVariance()
        
        return TouchFeatures(
            x = finalX,
            y = finalY,
            pressure = pressure,
            size = size,
            dwellTime = dwellTime,
            velocity = velocity,
            curvature = curvature,
            pressureVariance = pressureVariance,
            sizeVariance = sizeVariance,
            distance = distance
        )
    }
    
    private fun calculateCurvature(): Float {
        if (touchHistory.size < 3) {
            return 0f
        }
        
        var totalCurvature = 0f
        var count = 0
        
        for (i in 1 until touchHistory.size - 1) {
            val prev = touchHistory[i - 1]
            val curr = touchHistory[i]
            val next = touchHistory[i + 1]
            
            // Calculate angle between three points
            val angle = calculateAngle(prev, curr, next)
            totalCurvature += abs(angle - 180f) // Deviation from straight line
            count++
        }
        
        return if (count > 0) totalCurvature / count else 0f
    }
    
    private fun calculateAngle(p1: TouchPoint, p2: TouchPoint, p3: TouchPoint): Float {
        val a = sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y))
        val b = sqrt((p3.x - p2.x) * (p3.x - p2.x) + (p3.y - p2.y) * (p3.y - p2.y))
        val c = sqrt((p3.x - p1.x) * (p3.x - p1.x) + (p3.y - p1.y) * (p3.y - p1.y))
        
        if (a == 0f || b == 0f) return 0f
        
        val cosAngle = (a * a + b * b - c * c) / (2 * a * b)
        val clampedCos = cosAngle.coerceIn(-1f, 1f)
        
        return kotlin.math.acos(clampedCos) * 180f / kotlin.math.PI.toFloat()
    }
    
    private fun calculatePressureVariance(): Float {
        if (touchHistory.isEmpty()) return 0f
        
        val pressures = touchHistory.map { it.pressure }
        val mean = pressures.average().toFloat()
        val variance = pressures.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return variance
    }
    
    private fun calculateSizeVariance(): Float {
        if (touchHistory.isEmpty()) return 0f
        
        val sizes = touchHistory.map { it.size }
        val mean = sizes.average().toFloat()
        val variance = sizes.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return variance
    }
    
    fun getFeatureVector(): DoubleArray? {
        val features = _touchFeatures.value ?: return null
        
        return doubleArrayOf(
            features.x.toDouble(),
            features.y.toDouble(),
            features.pressure.toDouble(),
            features.size.toDouble(),
            features.dwellTime.toDouble(),
            features.velocity.toDouble(),
            features.curvature.toDouble(),
            features.pressureVariance.toDouble(),
            features.sizeVariance.toDouble(),
            features.distance.toDouble()
        )
    }
    
    fun clearFeatures(): Unit {
        _touchFeatures.value = null
    }
}

/**
 * Touch point data class
 */
data class TouchPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val size: Float,
    val timestamp: Long
)

/**
 * Touch features data class
 */
data class TouchFeatures(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val size: Float,
    val dwellTime: Float,
    val velocity: Float,
    val curvature: Float,
    val pressureVariance: Float,
    val sizeVariance: Float,
    val distance: Float
)
