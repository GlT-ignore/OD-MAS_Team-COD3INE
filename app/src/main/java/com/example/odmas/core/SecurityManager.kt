package com.example.odmas.core

import android.content.Context
import android.util.Log
import com.example.odmas.core.agents.*
import com.example.odmas.core.chaquopy.ChaquopyBehavioralManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.odmas.utils.LogFileLogger
import kotlinx.coroutines.CoroutineExceptionHandler

class SecurityManager(private val context: Context) {
    private val tier0Agent = Tier0StatsAgent()
    private val tier1Agent = Tier1BehaviorAgent(context)
    private val fusionAgent = FusionAgent()
    private val policyAgent = PolicyAgent()
    private val chaquopyManager = ChaquopyBehavioralManager.getInstance(context)
    private val _securityState = MutableStateFlow(SecurityState())
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()
    private val job = kotlinx.coroutines.SupervisorJob()
    private val crashHandler = CoroutineExceptionHandler { _, t ->
        LogFileLogger.log(TAG, "Coroutine error: ${t.message}", t)
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job + crashHandler)
    private var isInitialized = false
    private var lastFeatures: DoubleArray? = null
    private var lastModality: Modality? = null
    private var stateTickerJob: kotlinx.coroutines.Job? = null
    private var calibrationMode = false
    private var testMode = false

    companion object {
        private const val PROCESSING_INTERVAL_MS = 3000L
        private const val TAG = "SecurityManager"
        @Volatile private var instance: SecurityManager? = null
        fun getInstance(context: Context): SecurityManager {
            val appCtx = context.applicationContext
            val current = instance
            if (current != null) return current
            return synchronized(this) {
                instance ?: SecurityManager(appCtx).also { instance = it }
            }
        }
    }

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting security manager initialization...")
                LogFileLogger.init(context)
                LogFileLogger.log(TAG, "Initialization started")
                val chaquopyInitialized = chaquopyManager.initialize()
                Log.d(TAG, "Chaquopy Python ML initialization: $chaquopyInitialized")
                val tier1Initialized = tier1Agent.initializeModel()
                Log.d(TAG, "Tier-1 agent initialization: $tier1Initialized")
                fusionAgent.initializeSession()
                Log.d(TAG, "Fusion agent initialized")
                policyAgent.reset()
                Log.d(TAG, "Policy agent reset")
                if (chaquopyInitialized) {
                    chaquopyManager.startMonitoring()
                    Log.d(TAG, "Chaquopy monitoring started")
                }
                isInitialized = true
                updateSecurityState()
                Log.d(TAG, "Security manager initialization completed successfully")
                // Periodic ticker to refresh calibration progress/risk UI
                stateTickerJob?.cancel()
                stateTickerJob = coroutineScope.launch {
                    while (isInitialized) {
                        runCatching { updateSecurityState() }
                        kotlinx.coroutines.delay(1000L)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error during initialization: ${e.message}", e)
                isInitialized = true
                updateSecurityState()
                Log.d(TAG, "Security manager initialized with fallback mode")
                stateTickerJob?.cancel()
                stateTickerJob = coroutineScope.launch {
                    while (isInitialized) {
                        runCatching { updateSecurityState() }
                        kotlinx.coroutines.delay(1000L)
                    }
                }
                true
            }
        }
    }

    fun processSensorData(features: DoubleArray): Unit = processSensorData(features, Modality.UNKNOWN)

    fun processSensorData(features: DoubleArray, modality: Modality): Unit {
        if (!isInitialized) {
            Log.w(TAG, "Security manager not initialized, ignoring sensor data")
            return
        }
        Log.d(TAG, "Processing sensor data: ${features.contentToString()} (modality=$modality)")
        coroutineScope.launch {
            try {
                val learningDone = tier1Agent.isCalibrated()
                
                // During calibration mode, only collect data - don't calculate risk
                if (calibrationMode || !learningDone) {
                    Log.d(TAG, "Calibration mode: collecting data only")
                    tier0Agent.addFeatures(features, modality)
                    tier1Agent.submitCalibrationSample(features, modality)
                    tier1Agent.trainAllIfNeeded()
                    
                    // Add to Chaquopy ML baseline training data
                    chaquopyManager.addBaselineSample(features)
                    val (currentSamples, targetSamples) = chaquopyManager.getBaselineProgress()
                    Log.d(TAG, "Chaquopy ML baseline progress: $currentSamples/$targetSamples samples")
                    
                    lastFeatures = features
                    lastModality = modality
                    updateSecurityState(0.0, PolicyAction.Monitor)
                    return@launch
                }
                
                // Skip risk calculation if not in test mode after calibration
                if (!testMode && learningDone) {
                    Log.d(TAG, "Calibration complete but not in test mode - standby")
                    updateSecurityState(0.0, PolicyAction.Monitor)
                    return@launch
                }
                val chaquopyResult = chaquopyManager.analyzeBehavior(features)
                Log.d(TAG, "Chaquopy analysis: Risk=${chaquopyResult.riskScore}%, Confidence=${chaquopyResult.confidence}%")
                LogFileLogger.log(TAG, "Chaquopy risk=${chaquopyResult.riskScore}, conf=${chaquopyResult.confidence}")
                tier0Agent.addFeatures(features, modality)
                lastFeatures = features
                lastModality = modality
                val mahalanobisDistance = tier0Agent.computeMahalanobisDistance()
                if (mahalanobisDistance != null) {
                    Log.d(TAG, "Tier-0 Mahalanobis distance: $mahalanobisDistance")
                    val tier0Risk = fusionAgent.processTier0Risk(mahalanobisDistance)
                    Log.d(TAG, "Tier-0 risk: $tier0Risk")
                    var tier1Risk: Double? = null
                    if (fusionAgent.shouldRunTier1(tier0Risk)) {
                        // Only use the current modality for risk calculation
                        when (modality) {
                            Modality.TOUCH -> {
                                val touchW = tier0Agent.getWindowFeaturesFor(Modality.TOUCH)
                                try { 
                                    if (touchW != null) {
                                        tier1Risk = tier1Agent.computeTier1Probability(touchW, Modality.TOUCH)
                                        Log.d(TAG, "Tier-1 touch risk: $tier1Risk")
                                    }
                                } catch (e: Throwable) {
                                    Log.e(TAG, "Touch risk calculation error: ${e.message}")
                                }
                            }
                            Modality.TYPING -> {
                                val typingW = tier0Agent.getWindowFeaturesFor(Modality.TYPING)
                                try {
                                    if (typingW != null) {
                                        tier1Risk = tier1Agent.computeTier1Probability(typingW, Modality.TYPING)
                                        Log.d(TAG, "Tier-1 typing risk: $tier1Risk")
                                    }
                                } catch (e: Throwable) {
                                    Log.e(TAG, "Typing risk calculation error: ${e.message}")
                                }
                            }
                            else -> {
                                Log.d(TAG, "Unknown modality, skipping Tier-1 risk calculation")
                            }
                        }
                        if (tier1Risk != null) fusionAgent.markTier1Run()
                    }
                    val fusedRisk = fusionAgent.fuseRisks(tier0Risk, tier1Risk)
                    Log.d(TAG, "=== FUSION CALCULATION ===")
                    Log.d(TAG, "Input risks - Tier0: $tier0Risk, Tier1: $tier1Risk")
                    Log.d(TAG, "Fused risk result: $fusedRisk")
                    Log.d(TAG, "Chaquopy confidence: ${chaquopyResult.confidence}%")
                    
                    val sessionRisk = if (chaquopyResult.confidence > 80f) {
                        val chaquopyWeight = 0.5
                        val fusedWeight = 0.5
                        val weightedResult = fusedWeight * fusedRisk + chaquopyWeight * chaquopyResult.riskScore.toDouble()
                        Log.d(TAG, "HIGH CONFIDENCE: Using weighted combination")
                        Log.d(TAG, "  Fused risk: $fusedRisk (weight: $fusedWeight)")
                        Log.d(TAG, "  Chaquopy risk: ${chaquopyResult.riskScore} (weight: $chaquopyWeight)")
                        Log.d(TAG, "  Final weighted: $weightedResult")
                        weightedResult
                    } else {
                        Log.d(TAG, "LOW CONFIDENCE: Using fused risk only")
                        Log.d(TAG, "  Final risk: $fusedRisk")
                        fusedRisk
                    }
                    Log.d(TAG, "=== FINAL SESSION RISK: $sessionRisk ===")
                    LogFileLogger.log(TAG, "Fused session risk: $sessionRisk, tier0Ready=${tier0Agent.isBaselineReady()}, tier1Ready=${tier1Agent.isAnyModalityReady()}")
                    val policyAction = policyAgent.processSessionRisk(sessionRisk)
                    updateSecurityState(sessionRisk, policyAction)
                    if (policyAction == PolicyAction.Escalate) handleEscalation()
                } else {
                    Log.d(TAG, "=== FALLBACK: MAHALANOBIS NOT AVAILABLE ===")
                    Log.d(TAG, "Using Chaquopy risk only: ${chaquopyResult.riskScore}")
                    val sessionRisk = chaquopyResult.riskScore.toDouble()
                    Log.d(TAG, "=== FINAL SESSION RISK: $sessionRisk ===")
                    val policyAction = policyAgent.processSessionRisk(sessionRisk)
                    updateSecurityState(sessionRisk, policyAction)
                    if (policyAction == PolicyAction.Escalate) handleEscalation()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "processSensorData error: ${t.message}", t)
                LogFileLogger.log(TAG, "processSensorData error: ${t.message}", t)
            }
        }
    }

    fun onBiometricSuccess(): Unit {
        Log.d(TAG, "=== BIOMETRIC SUCCESS ===")
        Log.d(TAG, "Resetting policy state and trust credits")
        policyAgent.onBiometricSuccess()
        // Force update with zero session risk
        updateSecurityState(sessionRisk = 0.0, policyAction = PolicyAction.Monitor)
        val currentState = _securityState.value
        Log.d(TAG, "Post-success state: risk=${currentState.sessionRisk}, escalated=${currentState.isEscalated}, credits=${currentState.trustCredits}")
        Log.d(TAG, "=== BIOMETRIC SUCCESS COMPLETE ===")
    }

    fun onBiometricFailure(): Unit {
        Log.d(TAG, "=== BIOMETRIC FAILURE ===")
        Log.d(TAG, "Keeping escalated state for retry")
        policyAgent.onBiometricFailure()
        updateSecurityState()
        val currentState = _securityState.value
        Log.d(TAG, "Post-failure state: risk=${currentState.sessionRisk}, escalated=${currentState.isEscalated}, credits=${currentState.trustCredits}")
        Log.d(TAG, "=== BIOMETRIC FAILURE COMPLETE ===")
    }

    fun reset(): Unit {
        tier0Agent.resetBaseline()
        tier1Agent.resetBaseline()
        fusionAgent.initializeSession()
        policyAgent.reset()
        chaquopyManager.resetModels()
        calibrationMode = false
        testMode = false
        updateSecurityState()
        Log.d(TAG, "All models and baseline data reset")
    }
    
    fun setCalibrationMode(enabled: Boolean) {
        calibrationMode = enabled
        Log.d(TAG, "Calibration mode: $calibrationMode")
        if (!enabled) {
            // Calibration complete - finalize all models including Chaquopy ML
            coroutineScope.launch {
                tier1Agent.trainAllIfNeeded()
                Log.d(TAG, "Tier-1 models trained")
                
                // Train Chaquopy ML models
                val mlTrainingSuccess = chaquopyManager.trainModels()
                Log.d(TAG, "Chaquopy ML training completed: $mlTrainingSuccess")
                
                Log.d(TAG, "All calibration models trained successfully")
            }
        }
    }
    
    fun setTestMode(enabled: Boolean) {
        testMode = enabled
        Log.d(TAG, "Test mode: $testMode")
    }

    fun seedDemoBaseline(): Unit {
        repeat(120) {
            val features = DoubleArray(10) { Math.random() * 0.5 + 0.25 }
            val modality = if (it % 2 == 0) Modality.TOUCH else Modality.TYPING
            tier0Agent.addFeatures(features, modality)
            tier1Agent.addBaselineSample(features, modality)
        }
        tier1Agent.trainAllIfNeeded()
        updateSecurityState()
    }

    fun getCurrentStatus(): SecurityStatus {
        val state = _securityState.value
        return SecurityStatus(
            isMonitoring = isInitialized,
            sessionRisk = state.sessionRisk,
            riskLevel = state.riskLevel,
            isEscalated = state.isEscalated,
            trustCredits = state.trustCredits,
            tier0Ready = tier0Agent.isBaselineReady(),
            tier1Ready = tier1Agent.isAnyModalityReady()
        )
    }

    fun cleanup(): Unit {
        tier1Agent.close()
        runCatching { stateTickerJob?.cancel() }
        job.cancel()
    }

    /**
     * Receive fine-grained typing timing from UI to help calibration when a11y is not available.
     */
    fun onTypingEvent(dwellMs: Long, flightMs: Long, isSpace: Boolean) {
        tier1Agent.submitTypingTiming(dwellMs, flightMs, isSpace)
    }
    
    /**
     * Track characters typed in UI text fields during calibration
     */
    fun onCharacterTyped(character: Char) {
        if (calibrationMode) {
            // Count characters during calibration
            val dwellMs = 150L // Estimated for UI typing
            val flightMs = 50L  // Estimated
            val isSpace = (character == ' ')
            tier1Agent.submitTypingTiming(dwellMs, flightMs, isSpace)
            Log.d(TAG, "Character typed during calibration: '$character' (${tier1Agent.getTypingProgress()})")
        }
    }

    private fun updateSecurityState(
        sessionRisk: Double = _securityState.value.sessionRisk,
        policyAction: PolicyAction = PolicyAction.Monitor
    ): Unit {
        val policyState = policyAgent.getCurrentState()

        val tier0Ready = tier0Agent.isBaselineReady()
        val learningDone = tier1Agent.isCalibrated()
        val isLearning = !learningDone
        val remainingPercent = if (learningDone) 0 else (100 - tier1Agent.getCalibrationProgressPercent()).coerceIn(0, 100)

        val (touchCount, touchTarget) = tier1Agent.getTouchProgress()
        val (typingCount, typingTarget) = tier1Agent.getTypingProgress()
        val calStage = tier1Agent.getCurrentCalibrationStage().name

        val touchStats = tier0Agent.getWindowStatsFor(Modality.TOUCH)
        val typingStats = tier0Agent.getWindowStatsFor(Modality.TYPING)
        val boundsStr = formatBoundsAll(touchStats, typingStats)

        _securityState.value = SecurityState(
            sessionRisk = sessionRisk,
            riskLevel = policyState.riskLevel,
            isEscalated = policyState.isEscalated,
            trustCredits = policyState.trustCredits,
            consecutiveHighRisk = policyState.consecutiveHighRisk,
            consecutiveLowRisk = policyState.consecutiveLowRisk,
            policyAction = policyAction,
            tier0Ready = tier0Ready,
            tier1Ready = tier1Agent.isAnyModalityReady(),
            isLearning = isLearning,
            baselineProgressSec = remainingPercent,
            baselineBounds = boundsStr,
            calibrationStage = calStage,
            touchCount = touchCount,
            touchTarget = touchTarget,
            typingCount = typingCount,
            typingTarget = typingTarget
        )
    }

    private fun handleEscalation(): Unit {
        updateSecurityState()
    }

    private fun formatBoundsAll(
        touch: Pair<DoubleArray, DoubleArray>?,
        typing: Pair<DoubleArray, DoubleArray>?
    ): String {
        val tokens = ArrayList<String>(30)
        fun appendPair(pair: Pair<DoubleArray, DoubleArray>?) {
            if (pair == null) {
                repeat(10) { tokens.add("0.000|0.000") }
            } else {
                val (mean, std) = pair
                val n = kotlin.math.min(10, kotlin.math.min(mean.size, std.size))
                for (i in 0 until n) {
                    val mStr = java.lang.String.format(java.util.Locale.US, "%.3f", mean[i])
                    val sStr = java.lang.String.format(java.util.Locale.US, "%.3f", std[i].coerceAtLeast(1e-9))
                    tokens.add("$mStr|$sStr")
                }
                if (n < 10) repeat(10 - n) { tokens.add("0.000|0.000") }
            }
        }
        appendPair(touch)
        appendPair(typing)
        return tokens.joinToString(",")
    }
}

data class SecurityState(
    val sessionRisk: Double = 0.0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val isEscalated: Boolean = false,
    val trustCredits: Int = 3,
    val consecutiveHighRisk: Int = 0,
    val consecutiveLowRisk: Int = 0,
    val policyAction: PolicyAction = PolicyAction.Monitor,
    val tier0Ready: Boolean = false,
    val tier1Ready: Boolean = false,
    // Learning / calibration UI support
    val isLearning: Boolean = true,
    // Remaining percent (0..100)
    val baselineProgressSec: Int = 100,
    // CSV of mean|std tokens for 30 entries (10 per modality)
    val baselineBounds: String = "",
    // Calibration staged guidance
    val calibrationStage: String = "TOUCH",
    val touchCount: Int = 0,
    val touchTarget: Int = 30,
    val typingCount: Int = 0,
    val typingTarget: Int = 100
)

data class SecurityStatus(
    val isMonitoring: Boolean,
    val sessionRisk: Double,
    val riskLevel: RiskLevel,
    val isEscalated: Boolean,
    val trustCredits: Int,
    val tier0Ready: Boolean,
    val tier1Ready: Boolean
)
