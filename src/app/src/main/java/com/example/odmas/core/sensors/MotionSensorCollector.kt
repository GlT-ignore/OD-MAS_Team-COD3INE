package com.example.odmas.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Motion Sensor Collector: Captures accelerometer and gyroscope data
 * 
 * Features extracted:
 * - Acceleration magnitude and direction
 * - Angular velocity
 * - Motion patterns and micro-tremors
 * - Device orientation changes
 */
class MotionSensorCollector(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    private val _motionFeatures = MutableStateFlow<MotionFeatures?>(null)
    val motionFeatures: StateFlow<MotionFeatures?> = _motionFeatures.asStateFlow()
    
    private val accelerometerHistory = mutableListOf<AccelerometerData>()
    private val gyroscopeHistory = mutableListOf<GyroscopeData>()
    private val maxHistorySize = 50
    
    private var lastAccelerometerTime: Long = 0L
    private var lastGyroscopeTime: Long = 0L
    
    companion object {
        private const val SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL
        private const val MIN_INTERVAL_MS = 100L // 10Hz sampling
    }
    
    /**
     * Start sensor monitoring
     */
    fun startMonitoring(): Boolean {
        var anyRegistered = false
        accelerometer?.let {
            val ok = sensorManager.registerListener(this, it, SAMPLING_RATE)
            anyRegistered = anyRegistered || ok
        }
        gyroscope?.let {
            val ok = sensorManager.registerListener(this, it, SAMPLING_RATE)
            anyRegistered = anyRegistered || ok
        }
        return anyRegistered
    }
    
    /**
     * Stop sensor monitoring
     */
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        accelerometerHistory.clear()
        gyroscopeHistory.clear()
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometerEvent(event)
            }
            Sensor.TYPE_GYROSCOPE -> {
                handleGyroscopeEvent(event)
            }
        }
        
        // Process features if we have enough data from either sensor
        if (accelerometerHistory.size >= 10 || gyroscopeHistory.size >= 10) {
            processMotionFeatures()
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used for this implementation
    }
    
    private fun handleAccelerometerEvent(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        
        // Rate limiting
        if (currentTime - lastAccelerometerTime < MIN_INTERVAL_MS) {
            return
        }
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        accelerometerHistory.add(AccelerometerData(x, y, z, currentTime))
        if (accelerometerHistory.size > maxHistorySize) {
            accelerometerHistory.removeAt(0)
        }
        
        lastAccelerometerTime = currentTime
    }
    
    private fun handleGyroscopeEvent(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        
        // Rate limiting
        if (currentTime - lastGyroscopeTime < MIN_INTERVAL_MS) {
            return
        }
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        gyroscopeHistory.add(GyroscopeData(x, y, z, currentTime))
        if (gyroscopeHistory.size > maxHistorySize) {
            gyroscopeHistory.removeAt(0)
        }
        
        lastGyroscopeTime = currentTime
    }
    
    private fun processMotionFeatures() {
        val accelReady = accelerometerHistory.size >= 10
        val gyroReady = gyroscopeHistory.size >= 10
        if (!accelReady && !gyroReady) return

        val accelFeatures = if (accelReady) {
            calculateAccelerometerFeatures()
        } else {
            AccelerometerFeatures(0.0, 0.0, 0.0)
        }
        val gyroFeatures = if (gyroReady) {
            calculateGyroscopeFeatures()
        } else {
            GyroscopeFeatures(0.0, 0.0, 0.0)
        }

        val features = MotionFeatures(
            accelerationMagnitude = accelFeatures.magnitude,
            accelerationVariance = accelFeatures.variance,
            accelerationPeak = accelFeatures.peak,
            angularVelocityMagnitude = gyroFeatures.magnitude,
            angularVelocityVariance = gyroFeatures.variance,
            angularVelocityPeak = gyroFeatures.peak,
            motionIntensity = accelFeatures.magnitude + gyroFeatures.magnitude,
            tremorLevel = calculateTremorLevel(),
            orientationChange = calculateOrientationChange()
        )
        _motionFeatures.value = features
    }
    
    private fun calculateAccelerometerFeatures(): AccelerometerFeatures {
        val recentData = accelerometerHistory.takeLast(10)
        
        // Calculate magnitude for each reading
        val magnitudes: List<Double> = recentData.map { data ->
            sqrt((data.x * data.x + data.y * data.y + data.z * data.z).toDouble())
        }
        
        val mean = magnitudes.average()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average()
        val peak = magnitudes.maxOrNull() ?: 0.0
        
        return AccelerometerFeatures(
            magnitude = mean,
            variance = variance,
            peak = peak
        )
    }
    
    private fun calculateGyroscopeFeatures(): GyroscopeFeatures {
        val recentData = gyroscopeHistory.takeLast(10)
        
        // Calculate magnitude for each reading
        val magnitudes: List<Double> = recentData.map { data ->
            sqrt((data.x * data.x + data.y * data.y + data.z * data.z).toDouble())
        }
        
        val mean = magnitudes.average()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average()
        val peak = magnitudes.maxOrNull() ?: 0.0
        
        return GyroscopeFeatures(
            magnitude = mean,
            variance = variance,
            peak = peak
        )
    }
    
    private fun calculateTremorLevel(): Double {
        if (accelerometerHistory.size < 20) {
            return 0.0
        }
        
        val recentData = accelerometerHistory.takeLast(20)
        
        // Calculate high-frequency components (tremors)
        var tremorSum = 0.0
        for (i in 1 until recentData.size) {
            val current = recentData[i]
            val previous = recentData[i - 1]
            
            val deltaX = abs(current.x - previous.x)
            val deltaY = abs(current.y - previous.y)
            val deltaZ = abs(current.z - previous.z)
            
            tremorSum += deltaX + deltaY + deltaZ
        }
        
        return tremorSum / (recentData.size - 1)
    }
    
    private fun calculateOrientationChange(): Double {
        if (accelerometerHistory.size < 10) {
            return 0.0
        }
        
        val recentData = accelerometerHistory.takeLast(10)
        
        // Calculate orientation change based on gravity vector changes
        var orientationChange = 0.0
        for (i in 1 until recentData.size) {
            val current = recentData[i]
            val previous = recentData[i - 1]
            
                    val currentGravity = sqrt((current.x * current.x + current.y * current.y + current.z * current.z).toDouble())
            val previousGravity = sqrt((previous.x * previous.x + previous.y * previous.y + previous.z * previous.z).toDouble())
            
            orientationChange += abs(currentGravity - previousGravity)
        }
        
        return orientationChange / (recentData.size - 1)
    }
    
    fun getFeatureVector(): DoubleArray? {
        val features = _motionFeatures.value ?: return null
        
        val arr = doubleArrayOf(
            features.accelerationMagnitude,
            features.accelerationVariance,
            features.accelerationPeak,
            features.angularVelocityMagnitude,
            features.angularVelocityVariance,
            features.angularVelocityPeak,
            features.motionIntensity,
            features.tremorLevel,
            features.orientationChange,
            0.0 // Placeholder for 10th feature to match Tier-0 requirements
        )
        for (i in arr.indices) {
            val v = arr[i]
            if (v.isNaN() || v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY) {
                arr[i] = 0.0
            }
        }
        return arr
    }
    
    fun clearFeatures() {
        _motionFeatures.value = null
    }
}

/**
 * Accelerometer data class
 */
data class AccelerometerData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

/**
 * Gyroscope data class
 */
data class GyroscopeData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

/**
 * Accelerometer features
 */
data class AccelerometerFeatures(
    val magnitude: Double,
    val variance: Double,
    val peak: Double
)

/**
 * Gyroscope features
 */
data class GyroscopeFeatures(
    val magnitude: Double,
    val variance: Double,
    val peak: Double
)

/**
 * Motion features data class
 */
data class MotionFeatures(
    val accelerationMagnitude: Double,
    val accelerationVariance: Double,
    val accelerationPeak: Double,
    val angularVelocityMagnitude: Double,
    val angularVelocityVariance: Double,
    val angularVelocityPeak: Double,
    val motionIntensity: Double,
    val tremorLevel: Double,
    val orientationChange: Double
)
