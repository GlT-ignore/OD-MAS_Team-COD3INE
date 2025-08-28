package com.example.odmas.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.odmas.core.SecurityManager
import com.example.odmas.core.SecurityState
import com.example.odmas.core.agents.PolicyAction
import com.example.odmas.core.sensors.MotionSensorCollector
import com.example.odmas.core.sensors.TouchSensorCollector
import com.example.odmas.core.services.SecurityMonitoringService
import com.example.odmas.core.Modality
import androidx.datastore.preferences.core.*
import com.example.odmas.core.data.securityDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Use shared singleton DataStore

/**
 * Security ViewModel: Manages security state and coordinates UI with security manager
 */
class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    
    // Expose for calibration helpers
    internal val securityManager = SecurityManager.getInstance(application)
    private val touchCollector = TouchSensorCollector()
    // Motion temporarily disabled
    // private val motionCollector = MotionSensorCollector(application)
    
    private val _uiState = MutableStateFlow(SecurityUIState())
    val uiState: StateFlow<SecurityUIState> = _uiState.asStateFlow()
    
    private val _biometricPromptState = MutableStateFlow<BiometricPromptState?>(null)
    val biometricPromptState: StateFlow<BiometricPromptState?> = _biometricPromptState.asStateFlow()
    // Fallback receiver so TOUCH_DATA advances calibration even if service is not active
    private val touchDataReceiver: android.content.BroadcastReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.example.odmas.TOUCH_DATA") {
                val features: DoubleArray? = intent.getDoubleArrayExtra("features")
                features?.let { securityManager.processSensorData(it, com.example.odmas.core.Modality.TOUCH) }
            }
        }
    }
    
    // Debug logging
    companion object {
        private const val TAG = "SecurityViewModel"
        
        // DataStore keys (matching service keys)
        private val SESSION_RISK_KEY = doublePreferencesKey("session_risk")
        private val RISK_LEVEL_KEY = stringPreferencesKey("risk_level")
        private val IS_ESCALATED_KEY = booleanPreferencesKey("is_escalated")
        private val TRUST_CREDITS_KEY = intPreferencesKey("trust_credits")
        private val LAST_UPDATE_KEY = longPreferencesKey("last_update")
    }
    
    init {
        initializeSecurity()
        observeSecurityState()
        restorePersistedState()
        startBackgroundService()
        // Register fallback receiver (app process scope)
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                getApplication<Application>().registerReceiver(
                    touchDataReceiver,
                    android.content.IntentFilter("com.example.odmas.TOUCH_DATA"),
                    android.content.Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                getApplication<Application>().registerReceiver(
                    touchDataReceiver,
                    android.content.IntentFilter("com.example.odmas.TOUCH_DATA")
                )
            }
        }
    }
    
    private fun initializeSecurity(): Unit {
        Log.d(TAG, "Initializing security system...")
        viewModelScope.launch {
            val initialized = securityManager.initialize()
            Log.d(TAG, "Security manager initialization: $initialized")
            if (initialized) {
                // val motionStarted = motionCollector.startMonitoring()
                // Log.d(TAG, "Motion sensor monitoring started: $motionStarted")
                updateUIState(isInitialized = true)
                Log.d(TAG, "Security system initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize security manager")
            }
        }
    }
    
    private fun observeSecurityState(): Unit {
        viewModelScope.launch {
            securityManager.securityState.collect { securityState ->
                Log.d(TAG, "Security state updated: risk=${securityState.sessionRisk}, level=${securityState.riskLevel}, escalated=${securityState.isEscalated}")
                updateUIState(securityState = securityState)
                
                // Handle policy actions
                when (securityState.policyAction) {
                    PolicyAction.Escalate -> {
                        Log.w(TAG, "🚨 Policy action: ESCALATE - Risk=${securityState.sessionRisk}% - showing biometric prompt")
                        showBiometricPrompt()
                    }
                    PolicyAction.DeEscalate -> {
                        Log.i(TAG, "✅ Policy action: DE-ESCALATE - Risk=${securityState.sessionRisk}% - hiding biometric prompt")
                        hideBiometricPrompt()
                    }
                    PolicyAction.Monitor -> {
                        Log.d(TAG, "📊 Policy action: MONITOR - Risk=${securityState.sessionRisk}% - continuing monitoring")
                    }
                }
            }
        }
    }
    
    /**
     * Process touch event from UI
     */
    fun processTouchEvent(event: android.view.MotionEvent, viewWidth: Int, viewHeight: Int): Unit {
        Log.d(TAG, "Processing touch event: action=${event.action}, x=${event.x}, y=${event.y}")
        touchCollector.processTouchEvent(event, viewWidth, viewHeight)
        
        // Get touch features and send to security manager
        val touchFeatures = touchCollector.getFeatureVector()
        if (touchFeatures != null) {
            Log.d(TAG, "Touch features extracted: ${touchFeatures.contentToString()}")
            // Pass explicit modality for calibration pipelines
            securityManager.processSensorData(touchFeatures, Modality.TOUCH)
            touchCollector.clearFeatures()
        }
    }
    
    /**
     * Handle biometric verification success
     */
    fun onBiometricSuccess(): Unit {
        Log.i(TAG, "🔐 BIOMETRIC SUCCESS RECEIVED - calling security manager")
        val preState = _uiState.value.securityState
        Log.d(TAG, "Pre-success state: risk=${preState.sessionRisk}, escalated=${preState.isEscalated}")
        
        securityManager.onBiometricSuccess()
        hideBiometricPrompt()
        
        val postState = _uiState.value.securityState
        Log.i(TAG, "🔐 Post-success state: risk=${postState.sessionRisk}, escalated=${postState.isEscalated}")
        Log.i(TAG, "🔐 BIOMETRIC SUCCESS HANDLED COMPLETE")
    }
    
    /**
     * Handle biometric verification failure
     */
    fun onBiometricFailure(): Unit {
        Log.w(TAG, "🔐 BIOMETRIC FAILURE RECEIVED - calling security manager")
        val preState = _uiState.value.securityState
        Log.d(TAG, "Pre-failure state: risk=${preState.sessionRisk}, escalated=${preState.isEscalated}")
        
        securityManager.onBiometricFailure()
        // Keep prompt visible for retry
        
        val postState = _uiState.value.securityState
        Log.w(TAG, "🔐 Post-failure state: risk=${postState.sessionRisk}, escalated=${postState.isEscalated}")
        Log.w(TAG, "🔐 BIOMETRIC FAILURE HANDLED - PROMPT KEPT VISIBLE")
    }
    
    /**
     * Handle biometric verification cancellation
     */
    fun onBiometricCancelled(): Unit {
        Log.w(TAG, "🔐 BIOMETRIC CANCELLED RECEIVED - calling security manager")
        val preState = _uiState.value.securityState
        Log.d(TAG, "Pre-cancel state: risk=${preState.sessionRisk}, escalated=${preState.isEscalated}")
        
        securityManager.onBiometricFailure()
        hideBiometricPrompt()
        
        val postState = _uiState.value.securityState
        Log.w(TAG, "🔐 Post-cancel state: risk=${postState.sessionRisk}, escalated=${postState.isEscalated}")
        Log.w(TAG, "🔐 BIOMETRIC CANCELLED HANDLED COMPLETE")
    }
    
    /**
     * Reset security state (for demo mode)
     */
    fun resetSecurity(): Unit {
        Log.d(TAG, "Resetting security baseline")
        securityManager.reset()
        updateUIState()
        Log.d(TAG, "Security baseline reset complete")
    }
    
    /**
     * Toggle demo mode
     */
    fun toggleDemoMode(): Unit {
        val currentState = _uiState.value
        val newDemoMode = !currentState.isDemoMode
        Log.d(TAG, "Toggling demo mode: $newDemoMode")
        _uiState.value = currentState.copy(isDemoMode = newDemoMode)
        
        if (newDemoMode) {
            Log.d(TAG, "Demo mode enabled - simulating sensor data")
            // Seed baselines so agents become ready immediately
            securityManager.seedDemoBaseline()
            // Push a few simulated windows to show movement
            simulateSensorData()
        }
    }
    
    /**
     * Simulate sensor data for demo mode
     */
    private fun simulateSensorData(): Unit {
        viewModelScope.launch {
            Log.d(TAG, "Starting sensor data simulation")
            // Simulate some sensor data to trigger risk changes
            val simulatedTouchFeatures = doubleArrayOf(0.5, 0.3, 0.8, 0.2, 0.6, 0.4, 0.7, 0.1, 0.9, 0.5)
            
            Log.d(TAG, "Sending simulated touch features: ${simulatedTouchFeatures.contentToString()}")
            securityManager.processSensorData(simulatedTouchFeatures, Modality.TOUCH)
            
            kotlinx.coroutines.delay(1000) // Wait 1 second
            
            // Motion disabled
        }
    }
    
    /**
     * Delete local data
     */
    fun deleteLocalData(): Unit {
        securityManager.reset()
        // Additional cleanup would go here
        updateUIState()
    }
    
    /**
     * Start calibration flow - prepare system for calibration
     */
    fun startCalibrationFlow(): Unit {
        Log.d(TAG, "Starting calibration flow...")
        securityManager.reset() // Clear any existing data
        securityManager.setCalibrationMode(true) // Prevent risk calculation during calibration
        updateUIState()
    }
    
    /**
     * Complete calibration flow - build baseline and prepare for testing
     */
    fun completeCalibrationFlow(): Unit {
        Log.d(TAG, "Completing calibration flow...")
        securityManager.setCalibrationMode(false) // Enable risk calculation
        updateUIState()
    }
    
    /**
     * Start test mode - begin risk monitoring
     */
    fun startTestMode(): Unit {
        Log.d(TAG, "Starting test mode...")
        securityManager.setTestMode(true)
        updateUIState()
    }
    
    /**
     * Stop test mode
     */
    fun stopTestMode(): Unit {
        Log.d(TAG, "Stopping test mode...")
        securityManager.setTestMode(false)
        updateUIState()
    }
    
    private fun showBiometricPrompt(): Unit {
        val currentState = _uiState.value
        val reason = getBiometricReason(currentState.securityState.riskLevel)
        Log.w(TAG, "🔐 SHOWING BIOMETRIC PROMPT - Reason: $reason, RiskLevel: ${currentState.securityState.riskLevel}")
        _biometricPromptState.value = BiometricPromptState(
            isVisible = true,
            reason = reason
        )
    }
    
    private fun hideBiometricPrompt(): Unit {
        Log.i(TAG, "🔐 HIDING BIOMETRIC PROMPT")
        _biometricPromptState.value = null
    }
    
    private fun getBiometricReason(riskLevel: com.example.odmas.core.agents.RiskLevel): String {
        return when (riskLevel) {
            com.example.odmas.core.agents.RiskLevel.CRITICAL -> "Critical security risk detected"
            com.example.odmas.core.agents.RiskLevel.HIGH -> "Unusual behavior detected"
            com.example.odmas.core.agents.RiskLevel.MEDIUM -> "Behavioral anomaly detected"
            com.example.odmas.core.agents.RiskLevel.LOW -> "Security verification required"
        }
    }
    
    /**
     * Restore persisted security state from DataStore (for when app reopens)
     */
    private fun restorePersistedState(): Unit {
        viewModelScope.launch {
            try {
                val preferences = getApplication<Application>().securityDataStore.data.first()
                val lastUpdate = preferences[LAST_UPDATE_KEY] ?: 0L
                
                // Only restore if data is recent (within last 5 minutes)
                if (System.currentTimeMillis() - lastUpdate < 300000L) {
                    val sessionRisk = preferences[SESSION_RISK_KEY] ?: 0.0
                    val riskLevelName = preferences[RISK_LEVEL_KEY] ?: "LOW"
                    val isEscalated = preferences[IS_ESCALATED_KEY] ?: false
                    val trustCredits = preferences[TRUST_CREDITS_KEY] ?: 3
                    
                    val riskLevel = try {
                        com.example.odmas.core.agents.RiskLevel.valueOf(riskLevelName)
                    } catch (e: IllegalArgumentException) {
                        com.example.odmas.core.agents.RiskLevel.LOW
                    }
                    
                    val restoredState = SecurityState(
                        sessionRisk = sessionRisk,
                        riskLevel = riskLevel,
                        isEscalated = isEscalated,
                        trustCredits = trustCredits
                    )
                    
                    updateUIState(securityState = restoredState)
                    Log.d(TAG, "Restored persisted security state: risk=$sessionRisk, level=$riskLevel")
                } else {
                    Log.d(TAG, "Persisted state too old, using defaults")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore persisted state: ${e.message}")
            }
        }
    }
    
    private fun updateUIState(
        isInitialized: Boolean = _uiState.value.isInitialized,
        securityState: SecurityState = _uiState.value.securityState
    ): Unit {
        val keepDemoMode = _uiState.value.isDemoMode
        _uiState.value = SecurityUIState(
            isInitialized = isInitialized,
            securityState = securityState.copy(
                trustCredits = if (securityState.trustCredits == 0 && !isInitialized) 3 else securityState.trustCredits
            ),
            isDemoMode = keepDemoMode
        )
    }
    
    private fun startBackgroundService(): Unit {
        Log.d(TAG, "Starting background security service")
        val serviceIntent = Intent(getApplication(), SecurityMonitoringService::class.java)
        getApplication<Application>().startForegroundService(serviceIntent)
    }
    
    private fun stopBackgroundService(): Unit {
        Log.d(TAG, "Stopping background security service")
        val serviceIntent = Intent(getApplication(), SecurityMonitoringService::class.java)
        getApplication<Application>().stopService(serviceIntent)
    }
    
    override fun onCleared(): Unit {
        super.onCleared()
        // motionCollector.stopMonitoring()
        securityManager.cleanup()
        stopBackgroundService()
        runCatching { getApplication<Application>().unregisterReceiver(touchDataReceiver) }
    }
}

// ---- Calibration helper (non-simulated) ----
fun SecurityViewModel.submitCalibrationSample(features: DoubleArray, modality: Modality) {
    securityManager.processSensorData(features, modality)
}

/**
 * UI state for security interface
 */
data class SecurityUIState(
    val isInitialized: Boolean = false,
    val securityState: SecurityState = SecurityState(),
    val isDemoMode: Boolean = false
)

/**
 * Biometric prompt state
 */
data class BiometricPromptState(
    val isVisible: Boolean,
    val reason: String
)
