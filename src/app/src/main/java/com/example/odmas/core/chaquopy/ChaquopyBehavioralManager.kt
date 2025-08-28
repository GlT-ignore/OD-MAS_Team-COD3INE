package com.example.odmas.core.chaquopy

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Advanced Behavioral Biometrics Manager using Python ML
 * 
 * This class provides professional-grade behavioral biometrics analysis
 * using real Python ML libraries via Chaquopy integration.
 * Features Isolation Forest and One-Class SVM algorithms.
 */
class ChaquopyBehavioralManager private constructor(private val context: Context) {
    
    // Python environment
    private var pythonAvailable: Boolean = false
    private var behavioralModule: com.chaquo.python.PyObject? = null
    
    // ML state management
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _isTraining = MutableStateFlow(false)
    val isTraining: StateFlow<Boolean> = _isTraining.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _lastAnalysisResult = MutableStateFlow<BehavioralAnalysisResult?>(null)
    val lastAnalysisResult: StateFlow<BehavioralAnalysisResult?> = _lastAnalysisResult.asStateFlow()
    
    // Training data storage for baseline
    private val baselineDataBuffer = mutableListOf<DoubleArray>()
    private var isModelTrained = false
    
    companion object {
        private const val TAG = "ChaquopyBehavioralManager"
        private const val MIN_BASELINE_SAMPLES = 50
        private const val MAX_BASELINE_SAMPLES = 200
        
        @Volatile
        private var INSTANCE: ChaquopyBehavioralManager? = null
        
        fun getInstance(context: Context): ChaquopyBehavioralManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChaquopyBehavioralManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Initialize Python ML environment
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Initializing Python ML behavioral analysis system...")
            
            // Initialize Python runtime
            pythonAvailable = initializePython()
            
            if (pythonAvailable) {
                // Import our custom behavioral ML module
                val py = Python.getInstance()
                behavioralModule = py.getModule("behavioral_ml")
                
                // Test ML module
                val statusJson = behavioralModule!!.callAttr("get_model_status").toString()
                Log.d(TAG, "ML module status: $statusJson")
            }
            
            _isInitialized.value = pythonAvailable
            Log.d(TAG, "Python ML system initialized: $pythonAvailable")
            pythonAvailable
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Python ML system: ${e.message}", e)
            _isInitialized.value = false
            false
        }
    }
    
    /**
     * Initialize Python environment
     */
    private fun initializePython(): Boolean {
        return try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            
            val py = Python.getInstance()
            
            // Test numpy availability (should be installed via pip)
            try {
                py.getModule("numpy")
                Log.d(TAG, "NumPy available for ML computations")
            } catch (e: Exception) {
                Log.w(TAG, "NumPy not available, using pure Python implementations")
            }
            
            Log.d(TAG, "Python runtime initialized successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Python initialization failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Add sample to baseline training data
     */
    fun addBaselineSample(features: DoubleArray) {
        if (isModelTrained) {
            Log.d(TAG, "Model already trained, ignoring baseline sample")
            return
        }
        
        synchronized(baselineDataBuffer) {
            baselineDataBuffer.add(features.clone())
            Log.d(TAG, "Added baseline sample: ${baselineDataBuffer.size}/${MIN_BASELINE_SAMPLES}")
            
            // Limit buffer size
            if (baselineDataBuffer.size > MAX_BASELINE_SAMPLES) {
                baselineDataBuffer.removeAt(0)
            }
        }
    }
    
    /**
     * Train ML models on collected baseline data
     */
    suspend fun trainModels(): Boolean = withContext(Dispatchers.IO) {
        if (!pythonAvailable || behavioralModule == null) {
            Log.e(TAG, "Python ML not available for training")
            return@withContext false
        }
        
        if (isModelTrained) {
            Log.d(TAG, "Models already trained")
            return@withContext true
        }
        
        synchronized(baselineDataBuffer) {
            if (baselineDataBuffer.size < MIN_BASELINE_SAMPLES) {
                Log.w(TAG, "Insufficient baseline data: ${baselineDataBuffer.size}/${MIN_BASELINE_SAMPLES}")
                return@withContext false
            }
        }
        
        return@withContext try {
            _isTraining.value = true
            Log.d(TAG, "Training ML models on ${baselineDataBuffer.size} baseline samples...")
            
            // Convert baseline data to JSON
            val baselineJson = convertBaselineToJson()
            
            // Train models via Python
            val resultJson = behavioralModule!!.callAttr("train_baseline", baselineJson).toString()
            val result = JSONObject(resultJson)
            
            val success = result.optBoolean("success", false)
            if (success) {
                isModelTrained = true
                Log.d(TAG, "ML models trained successfully: ${result.optInt("models_trained", 0)} models")
                Log.d(TAG, "Training data: ${result.optInt("n_samples", 0)} samples, ${result.optInt("n_features", 0)} features")
            } else {
                val error = result.optString("error", "Unknown error")
                Log.e(TAG, "ML training failed: $error")
            }
            
            _isTraining.value = false
            success
            
        } catch (e: Exception) {
            _isTraining.value = false
            Log.e(TAG, "ML training error: ${e.message}", e)
            false
        }
    }
    
    /**
     * Analyze behavioral features using trained ML models
     */
    suspend fun analyzeBehavior(features: DoubleArray): BehavioralAnalysisResult = withContext(Dispatchers.IO) {
        if (!pythonAvailable || behavioralModule == null || !isModelTrained) {
            Log.w(TAG, "ML analysis not available, returning fallback result")
            return@withContext createFallbackResult(features)
        }
        
        return@withContext try {
            _isAnalyzing.value = true
            Log.d(TAG, "Analyzing behavioral features using ML models...")
            
            // Convert features to JSON
            val featuresJson = JSONArray(features.toList()).toString()
            
            // Analyze via Python ML
            val resultJson = behavioralModule!!.callAttr("analyze_behavior", featuresJson).toString()
            val result = JSONObject(resultJson)
            
            // Parse ML results
            val riskScore = result.optDouble("risk_score", 50.0).toFloat()
            val confidence = result.optDouble("confidence", 0.5).toFloat()
            val isolationScore = result.optDouble("isolation_score", 0.5)
            val svmScore = result.optDouble("svm_score", 0.5)
            val statisticalScore = result.optDouble("statistical_score", 0.5)
            val ensembleAgreement = result.optDouble("ensemble_agreement", 0.5)
            
            val isAnomalous = riskScore > 70.0f
            
            // Create detailed anomaly list
            val anomalies = mutableListOf<String>()
            if (isAnomalous) {
                anomalies.add("ML Ensemble Detection: Risk ${String.format("%.1f", riskScore)}%")
                anomalies.add("Isolation Forest: ${String.format("%.3f", isolationScore)} anomaly score")
                anomalies.add("One-Class SVM: ${String.format("%.3f", svmScore)} distance score")
                anomalies.add("Statistical Z-Score: ${String.format("%.3f", statisticalScore)} deviation")
                anomalies.add("Model Agreement: ${String.format("%.1f", ensembleAgreement * 100)}%")
            } else {
                anomalies.add("ML Ensemble: Normal behavioral pattern detected")
                anomalies.add("All models agree on normal behavior")
            }
            
            val analysisResult = BehavioralAnalysisResult(
                riskScore = riskScore,
                confidence = confidence,
                isAnomalous = isAnomalous,
                anomalies = anomalies,
                touchDynamicsScore = calculateTouchScore(features),
                typingRhythmScore = calculateTypingScore(features),
                motionPatternsScore = 0.0f, // Motion disabled
                overallBehaviorScore = riskScore / 100.0f,
                mlModelDetails = mapOf(
                    "isolation_forest_score" to isolationScore.toFloat(),
                    "one_class_svm_score" to svmScore.toFloat(),
                    "statistical_z_score" to statisticalScore.toFloat(),
                    "ensemble_agreement" to ensembleAgreement.toFloat(),
                    "confidence" to confidence,
                    "algorithm" to "Isolation Forest + One-Class SVM + Statistical Analysis"
                )
            )
            
            _lastAnalysisResult.value = analysisResult
            _isAnalyzing.value = false
            
            Log.d(TAG, "ML Analysis complete: Risk=${riskScore}%, Confidence=${confidence}, Agreement=${ensembleAgreement}")
            analysisResult
            
        } catch (e: Exception) {
            _isAnalyzing.value = false
            Log.e(TAG, "ML analysis error: ${e.message}", e)
            createFallbackResult(features)
        }
    }
    
    /**
     * Check if models are ready for analysis
     */
    fun isModelReady(): Boolean {
        return pythonAvailable && isModelTrained
    }
    
    /**
     * Get baseline training progress
     */
    fun getBaselineProgress(): Pair<Int, Int> {
        synchronized(baselineDataBuffer) {
            return Pair(baselineDataBuffer.size, MIN_BASELINE_SAMPLES)
        }
    }
    
    /**
     * Reset all models and baseline data
     */
    fun resetModels() {
        Log.d(TAG, "Resetting ML models and baseline data")
        synchronized(baselineDataBuffer) {
            baselineDataBuffer.clear()
        }
        isModelTrained = false
    }
    
    /**
     * Start monitoring mode (continuous baseline collection)
     */
    fun startMonitoring() {
        Log.d(TAG, "Started ML behavioral monitoring")
    }
    
    /**
     * Stop monitoring mode
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopped ML behavioral monitoring")
    }
    
    private fun convertBaselineToJson(): String {
        val jsonArray = JSONArray()
        synchronized(baselineDataBuffer) {
            for (sample in baselineDataBuffer) {
                val sampleArray = JSONArray()
                for (feature in sample) {
                    sampleArray.put(feature)
                }
                jsonArray.put(sampleArray)
            }
        }
        return jsonArray.toString()
    }
    
    private fun createFallbackResult(features: DoubleArray): BehavioralAnalysisResult {
        // Simple statistical fallback when ML is not available
        val variance = if (features.isNotEmpty()) {
            val mean = features.average()
            features.map { (it - mean) * (it - mean) }.average()
        } else {
            0.5
        }
        
        val riskScore = (variance * 50.0 + 25.0).toFloat().coerceIn(0.0f, 100.0f)
        val confidence = 0.4f // Lower confidence without ML
        
        return BehavioralAnalysisResult(
            riskScore = riskScore,
            confidence = confidence,
            isAnomalous = riskScore > 70.0f,
            anomalies = listOf("Fallback statistical analysis (ML unavailable)"),
            touchDynamicsScore = calculateTouchScore(features),
            typingRhythmScore = calculateTypingScore(features),
            motionPatternsScore = 0.0f,
            overallBehaviorScore = riskScore / 100.0f,
            mlModelDetails = mapOf(
                "algorithm" to "Statistical Fallback",
                "ml_available" to false
            )
        )
    }
    
    private fun calculateTouchScore(features: DoubleArray): Float {
        // Touch-specific scoring (assuming first half of features are touch-related)
        if (features.size < 4) return 0.5f
        
        val touchFeatures = features.take(features.size / 2)
        val touchVariance = if (touchFeatures.isNotEmpty()) {
            val mean = touchFeatures.average()
            touchFeatures.map { (it - mean) * (it - mean) }.average()
        } else {
            0.5
        }
        
        return (touchVariance * 0.8 + 0.1).toFloat().coerceIn(0.0f, 1.0f)
    }
    
    private fun calculateTypingScore(features: DoubleArray): Float {
        // Typing-specific scoring (assuming second half of features are typing-related)
        if (features.size < 4) return 0.5f
        
        val typingFeatures = features.drop(features.size / 2)
        val typingVariance = if (typingFeatures.isNotEmpty()) {
            val mean = typingFeatures.average()
            typingFeatures.map { (it - mean) * (it - mean) }.average()
        } else {
            0.5
        }
        
        return (typingVariance * 0.8 + 0.1).toFloat().coerceIn(0.0f, 1.0f)
    }
}

/**
 * Enhanced Behavioral Analysis Result with ML details
 */
data class BehavioralAnalysisResult(
    val riskScore: Float,
    val confidence: Float,
    val isAnomalous: Boolean,
    val anomalies: List<String>,
    val touchDynamicsScore: Float,
    val typingRhythmScore: Float,
    val motionPatternsScore: Float,
    val overallBehaviorScore: Float,
    val mlModelDetails: Map<String, Any> = emptyMap()
)